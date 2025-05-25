package io.github.czelabueno.jai.workflow;

import io.github.czelabueno.jai.workflow.graph.Format;
import io.github.czelabueno.jai.workflow.graph.StyleAttribute;
import io.github.czelabueno.jai.workflow.node.Conditional;
import io.github.czelabueno.jai.workflow.node.Node;
import io.github.czelabueno.jai.workflow.transition.ComputedTransition;
import io.github.czelabueno.jai.workflow.transition.Transition;
import io.github.czelabueno.jai.workflow.graph.GraphImageGenerator;
import io.github.czelabueno.jai.workflow.graph.graphviz.GraphvizImageGenerator;
import io.github.czelabueno.jai.workflow.transition.TransitionState;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

public class DefaultStateWorkflow<T> implements StateWorkflow<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultStateWorkflow.class);
    private final String name;
    private final Map<TransitionState, List<TransitionState>> adjList;
    private volatile Node<T,?> startNode;
    private final T statefulBean;
    private final List<Transition> transitions; // definition transitions
    private final Map<TransitionState, CounterTransitionsPerState> transitionsPerState;
    private final List<TransitionState> compiledStates;
    private volatile int executionOrder;
    private final List<ComputedTransition> computedTransitions; // computed transitions after running
    private boolean module;
    private final GraphImageGenerator graphImageGenerator;

    protected DefaultStateWorkflow(Builder<T> builder) {
        if (builder.statefulBean == null) {
            throw new IllegalArgumentException("Stateful bean cannot be null");
        }
        if (builder.addEdges == null || builder.addEdges.isEmpty()) {
            throw new IllegalArgumentException("At least one edged must be added to the workflow");
        }

        this.name = builder.name != null ? builder.name : "default";
        this.statefulBean = builder.statefulBean;
        this.adjList = new ConcurrentHashMap<>();
        this.transitions = Collections.synchronizedList(new ArrayList<>());
        this.computedTransitions = Collections.synchronizedList(new ArrayList<>());
        this.module = false;

        this.graphImageGenerator = builder.graphImageGenerator != null ? builder.graphImageGenerator : GraphvizImageGenerator.builder().build();

        // build transitions user definition
        this.transitionsPerState = new ConcurrentHashMap<>();
        buildDefinitionTransitions(builder.addEdges, builder.addNodes);

        // validate constraints
        this.compiledStates = Collections.synchronizedList(new ArrayList<>());
        compileValidation(WorkflowStateName.START);
    }

    private void buildDefinitionTransitions(List<Transition> edges, List<Node<T, ?>> nodes) {
        // Add edges to adjList
        this.adjList.clear();
        edges.forEach(transition -> {
            this.adjList.putIfAbsent(transition.from(), Collections.synchronizedList(new ArrayList<>()));
            this.adjList.putIfAbsent(transition.to(), Collections.synchronizedList(new ArrayList<>()));
            if (transition.from() instanceof Conditional conditionalFrom) { // Add expected nodes to adjList if the 'from' node is a Conditional
                this.adjList.get(conditionalFrom).addAll(conditionalFrom.getExpectedNodes());
            } else if (transition.to() instanceof Conditional conditionalTo) { // Add expected nodes to adjList if the 'to' node is a Conditional
                this.adjList.get(conditionalTo).addAll(conditionalTo.getExpectedNodes());
                this.adjList.get(transition.from()).add(conditionalTo); // Add the Conditional node to the adjList
            } else {
                this.adjList.get(transition.from()).add(transition.to()); // Add the edge to the adjList
            }
        });

        // Add nodes to adjList if they are not already present
        nodes.forEach(node -> this.adjList.putIfAbsent(node, Collections.synchronizedList(new ArrayList<>())));

        // Count the number of input and output transitions for each state
        this.transitionsPerState.clear();
        this.adjList.forEach((node, transitionStates) -> {
            transitionStates.forEach(transition -> this.transitionsPerState
                    .computeIfAbsent(transition, k -> new CounterTransitionsPerState(0, 0))
                    .incrementInputTransitions()); // the existing or new CounterTransitionsPerState is updated with the incremented input transitions
            this.transitionsPerState.computeIfAbsent(node, k -> new CounterTransitionsPerState(0, transitionStates.size()))
                    .setOutputTransitions(transitionStates.size());
        });


        if (!this.transitionsPerState.containsKey(WorkflowStateName.START)) {
            // Setting nodes without incoming transitions as first nodes
            List<TransitionState> firstStates = this.transitionsPerState.entrySet().stream()
                    .filter(counter -> counter.getValue().getInputTransitions() == 0)
                    .map(Map.Entry::getKey)
                    .collect(toList());

            this.adjList.putIfAbsent(WorkflowStateName.START, firstStates);
            this.transitionsPerState.putIfAbsent(WorkflowStateName.START, new CounterTransitionsPerState(0, firstStates.size()));
            firstStates.stream().forEach(firstState -> transitionsPerState.get(firstState).incrementInputTransitions());

        }
        if (!this.transitionsPerState.containsKey(WorkflowStateName.END)) {
            // Setting nodes without outgoing transitions as last nodes
            List<TransitionState> lastStates = this.transitionsPerState.entrySet().stream()
                    .filter(counter -> counter.getValue().getOutputTransitions() == 0)
                    .map(Map.Entry::getKey)
                    .collect(toList());

            lastStates.stream().forEach(lastState -> this.adjList.computeIfAbsent(lastState, k -> Collections.synchronizedList(new ArrayList<>())).add(WorkflowStateName.END));
            this.transitionsPerState.putIfAbsent(WorkflowStateName.END, new CounterTransitionsPerState(lastStates.size(), 0));
            lastStates.stream().forEach(lastState -> this.transitionsPerState.get(lastState).incrementOutputTransitions());
        }
        // Add all built transitions from adjList
        this.transitions.clear();
        this.adjList.forEach((node, ts) -> ts.forEach(transitionStateTo -> this.transitions.add(Transition.from(node, transitionStateTo))));
    }

    private void compileValidation(TransitionState state) {
        boolean isSplit = false;
        if (this.transitionsPerState.get(state).getOutputTransitions() > 1 && !(state instanceof Conditional)) {
            isSplit = true;
        };
        boolean isMerge = false;
        boolean isParallel = false;

        if (state instanceof Node) {
            Node node = (Node) state;
            isMerge = node.hasLabel("Merge");
            isParallel = node.hasLabel("Parallel");
            if (isParallel && isSplit) {
                throw new IllegalArgumentException("A parallel node '" + node.graphName() + "' cannot be a split node in the same flow");
            }
            if (isMerge && isSplit) {
                throw new IllegalArgumentException("A merge node '" + node.graphName() + "' cannot be a split node in the same flow");
            }
            if (isMerge && isParallel) {
                throw new IllegalArgumentException("A merge node '" + node.graphName() + "' cannot be a parallel node in the same flow");
            }
            if (isMerge) {
                int mergeInputTransitions = this.transitionsPerState.get(state).getInputTransitions(); // number of input transitions for merge node
                this.compiledStates.stream()
                        .filter(compiledState -> compiledState.hasLabel("Split"))
                        .findAny()
                        .ifPresent(existingSplitNode -> {
                            int splitOutputTransitions = this.transitionsPerState.get(existingSplitNode).getOutputTransitions(); // number of output transitions for split node
                            if (mergeInputTransitions != splitOutputTransitions) {
                                throw new IllegalArgumentException("The merge node '" + node.graphName() + "' must have the same number of input transitions as the number of output transitions from the split node '" + existingSplitNode.graphName() + "'");
                            }
                        });
            }
            if (isSplit) node.setLabels("Split");
        }
        this.compiledStates.add(state); // state compiled and validated

        List<TransitionState> nextStates = this.adjList.get(state);
        for (TransitionState nextState : nextStates) {
            if (!this.compiledStates.contains(nextState)) {
                if (nextState instanceof Node) {
                    Node<T, ?> targetNode = (Node<T, ?>) nextState;
                    if (isSplit || isParallel) {
                        targetNode.setLabels("Parallel");
                    }
                    if (this.transitionsPerState.get(targetNode).getInputTransitions() > 1 && this.transitions.stream()
                                .filter(transition -> transition.to().equals(targetNode) && transition.from() instanceof Conditional)
                                .findAny()
                                .isEmpty()){ // 'from' should not be a Conditional node
                        targetNode.labels().clear();
                        targetNode.setLabels("Merge");
                    }
                }
                if (isParallel){
                    if (nextState instanceof WorkflowStateName) {
                        throw new IllegalArgumentException("The state " + state.graphName() + " labeled as 'Parallel' cannot have a WorkflowStateName '" + nextState.graphName() +"' as an adjacent node");
                    } else if (!nextState.hasLabel("Merge") && !nextState.hasLabel("Parallel")) {
                        throw new IllegalArgumentException("A node labeled as 'Parallel' must have a node labeled as 'Merge' or 'Parallel' as an adjacent node");
                    }
                }
                if (nextState == WorkflowStateName.END) {
                    return;
                }
                compileValidation(nextState);
            }
        }
        // Constraint 7
        // This constraint requires runtime behavior, so it should be implemented in the `runNode` method.
    }

    @Override
    public void addNode(Node<T, ?> node) {
        adjList.putIfAbsent(node, Collections.synchronizedList(new ArrayList<>()));
    }

    @Override
    public void putEdge(Node<T, ?> from, Node<T, ?> to) {
        putEdgeIfAbsent(from, to);
    }

    @Override
    public void putEdge(Node<T, ?> from, Conditional<T> conditional) {
        putEdgeIfAbsent(from, conditional);
    }

    @Override
    public void putEdge(Node<T, ?> from, WorkflowStateName state) {
        putEdgeIfAbsent(from, state);
    }

    private void putEdgeIfAbsent(TransitionState from, TransitionState to) {
        // if the edge is already present, skip
        if (this.transitions.stream().noneMatch(transition -> transition.from().equals(from) && transition.to().equals(to))) {
            // 1. If the incoming 'from' transitionState already has an END state set, it will be removed to replace the new 'to' transitionState.
            this.transitions.removeIf(transition -> transition.from().equals(from) && transition.to() == WorkflowStateName.END);
            // 2. If the incoming 'to' transitionState is an explicit END state, the existing transition with END state will be removed and the incoming 'from' transitionState will be updated.
            if (to == WorkflowStateName.END) {
                this.transitions.removeIf(transition -> transition.to() == to);
                this.transitions.removeIf(transition -> transition.from().equals(from));
            }
            this.transitions.add(Transition.from(from, to));
            buildDefinitionTransitions(this.transitions, List.of()); // rebuild transitions, no nodes to add
            this.compiledStates.clear();
            compileValidation(WorkflowStateName.START); // recompile validation
        }
    }

    @Override
    public DefaultStateWorkflow startNode(Node<T,?> startNode){
        this.startNode = startNode;
        return this;
    }

    @Override
    public Node<T, ?> getLastNode() {
        if (this.adjList.isEmpty() || this.adjList == null)
            throw new IllegalStateException("No nodes added to the workflow");

        return this.adjList.entrySet().stream()
                .filter(entry -> entry.getValue().contains(WorkflowStateName.END))
                .map(Map.Entry::getKey)
                .filter(Node.class::isInstance)
                .<Node<T, ?>>map(node -> (Node<T, ?>) node)
                .reduce((first, second) -> second)
                .orElseGet(() -> this.transitionsPerState.entrySet().stream()
                                .filter(counter -> counter.getValue().getOutputTransitions() == 0)
                                .map(Map.Entry::getKey)
                                .filter(Node.class::isInstance)
                                .reduce((first, second) -> second)
                                .map(node -> (Node<T, ?>) node)
                                .orElseThrow(() -> new IllegalStateException("No nodes added to the workflow")));
    }

    @Override
    public T run() {
        return run(this.startNode, null);
    }

    @Override
    public T runStream(Consumer<Node<T, ?>> eventConsumer) {
        return run(this.startNode, eventConsumer);
    }

    private T run(Node<T,?> node, Consumer<Node<T, ?>> eventConsumer) {
        if (this.compiledStates == null || this.compiledStates.isEmpty()) {
            throw new IllegalStateException("jai workflow cannot run without a built workflow");
        }
        if (this.transitions == null || this.transitions.isEmpty()) {
            throw new IllegalStateException("jai workflow cannot run without edges defined");
        }
        List<Node> startNodes = this.adjList.get(WorkflowStateName.START).stream()
                .filter(transitionState -> transitionState instanceof Node)
                .map(transitionState -> (Node) transitionState)
                .toList();
        node = determineStartNode(node, startNodes);

        resetWorkflowState();
        log.debug("STARTING workflow{}..", eventConsumer != null ? " in stream mode" : "");
        runNode(node, eventConsumer);
        log.debug("END workflow..");
        return statefulBean;
    }

    private Node<T,?> determineStartNode(Node<T,?> node, List<Node> startNodes) {
        if (node == null) {
            if (startNodes.size() > 1) {
                throw new IllegalStateException("Its not possible to determine the start node, multiple start nodes found: " +
                        startNodes.stream().sorted(Comparator.comparing(startNodes::indexOf)).map(Node::getName).toList() +
                        "\nPlease specify the start node using the .startNode(Node<T> node) method");
            } else if (startNodes.size() == 1) {
                node = startNodes.get(0);
            }
        }
        return node;
    }

    private void resetWorkflowState() {
        this.computedTransitions.clear(); // clean previous transitions
        this.executionOrder=1;
    }

    private void runNode(Node<T,?> node, Consumer<Node<T, ?>> eventConsumer) {
        log.debug("Running node name: " + node.getName() + "..");
        synchronized (this.statefulBean) {
            node.execute(this.statefulBean);
        }
        if (eventConsumer != null) {
            eventConsumer.accept(node);
        }
        List<TransitionState> nextNodes;
        synchronized (this.adjList) {
            nextNodes = this.adjList.get(node);
        }

        if (node.hasLabel("Split")) {
            // Parallel execution
            log.debug("Running node in parallel..");
            nextNodes.parallelStream()
                    .forEach(nextNode -> {
                        if (nextNode instanceof Node next) {
                            computeTransition(node, next);
                            runNode(next, eventConsumer);
                        }
                    });
            // Wait for all parallel tasks to complete
            ComputedTransition ct = getComputedTransitions().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
            Node nodeMerge = ct.getTransition().to().hasLabel("Merge") ? (Node) ct.getTransition().to() : null;
            if (nodeMerge != null) {
                computeTransition(node, nodeMerge);
                runNode(nodeMerge, eventConsumer);
            }

        } else if (node.hasLabel("Merge")) {
            // Merge node waits for all parallel tasks to complete
            log.debug("Running node in merge mode..");
            for (TransitionState nextNode: nextNodes) {
                if (nextNode instanceof Node next) {
                    computeTransition(node, next);
                    runNode(next, eventConsumer);
                }
            }
        } else {
            // Sequential execution
            log.debug("Running node sequentially..");
            for (TransitionState nextNode : nextNodes) {
                if (nextNode instanceof WorkflowStateName next) {
                    if (next == WorkflowStateName.END) {
                        log.debug("Reached END state");
                        computeTransition(node, next);
                        return;
                    }
                } else if (nextNode instanceof Node next) {
                    computeTransition(node, next);
                    if (!next.hasLabel("Merge")) {
                        runNode(next, eventConsumer);
                    }
                } else if (nextNode instanceof Conditional next) {
                    computeTransition(node, next);
                    Node<T,?> conditionalNode = next.evaluate(this.statefulBean);
                    if (conditionalNode == null) {
                        throw new IllegalStateException("Conditional node returned null");
                    } else {
                        computeTransition(next, conditionalNode);
                        runNode(conditionalNode, eventConsumer);
                    }
                }
            }
        }
    }

    private void computeTransition(TransitionState from, TransitionState to) {
        this.transitions.stream()
                .filter(transition -> transition.from().equals(from) && transition.to().equals(to))
                .findAny()
                .ifPresent(transition -> {
                    synchronized (this.computedTransitions) {
                        Optional<Integer> existingOrder = this.computedTransitions.stream()
                                .filter(t -> t.getTransition().from().equals(from) || t.getTransition().to().equals(to))
                                .map(ComputedTransition::getOrder)
                                .findFirst();

                        int order = existingOrder.isPresent() ? existingOrder.get() : this.executionOrder++;
                        this.computedTransitions.add(ComputedTransition.from(order, transition));
                    }
                });
    }

    @Override
    public List<Transition> getTransitions() {
        return this.transitions;
    }

    @Override
    public List<ComputedTransition> getComputedTransitions() {
        if (!wasRun()) {
            throw new RuntimeException("Workflow has not been run yet. No transitions computed");
        }
        return this.computedTransitions.stream()
                .sorted(Comparator.comparing(ComputedTransition::getOrder))
                .collect(toUnmodifiableList());
    }

    /**
     * Convert a StateWorkflow as a Module.
     *
     * @return a StateWorkflow as a Module
     */
    @Override
    public StateWorkflow toModule() {
        this.module=true;
        return this;
    }

    /**
     * Returns true if the workflow has been run.
     *
     * @return true if the workflow has been run, false otherwise
     */
    public Boolean wasRun() {
        return !this.computedTransitions.isEmpty();
    }

    @Override
    public void generateComputedWorkflowImage(Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        GraphvizImageGenerator graphImageGenerator = GraphvizImageGenerator.builder()
                .computedTransitions(getComputedTransitions())
                .build();
        imageGenerator(graphImageGenerator, format, outputPath, styleAttributes);
    }

    @Override
    public BufferedImage generateComputedWorkflowBufferedImage(Format format, List<StyleAttribute> styleAttributes) throws RuntimeException {
        GraphvizImageGenerator graphImageGenerator = GraphvizImageGenerator.builder()
                .computedTransitions(getComputedTransitions())
                .build();
        return imageGenerator(graphImageGenerator, format, styleAttributes);
    }

    /**
     * Returns a string representation of the workflow.
     *
     * @return a string representation of the workflow
     */
    public String prettyTransitions() {
        StringBuilder sb = new StringBuilder();
        for (ComputedTransition transition : getComputedTransitions()) {
            sb.append("[")
                    .append(transition.getTransition())
                    .append(" {")
                    .append("Order: "+ transition.getOrder()+", ")
                    .append("ComputedAt: "+ transition.getComputedAt()+", ")
                    .append("Payload: "+ transition.getPayload() +", ")
                    .append("Thread: " + transition.getThread() + " }").append("]\n");
        }
        return sb.toString();
    }

    // TODO: add a new parameter to specify the graph style: Layout DOT[normal, Sketchviz], Mermaid, etc
    @Override
    public void generateWorkflowImage(Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        imageGenerator(this.graphImageGenerator, format, outputPath, styleAttributes);
    }

    @Override
    public BufferedImage generateWorkflowBufferedImage(Format format, List<StyleAttribute> styleAttributes) throws RuntimeException {
        return imageGenerator(this.graphImageGenerator, format, styleAttributes);
    }

    private BufferedImage imageGenerator(GraphImageGenerator graphImageGenerator, Format format, List<StyleAttribute> styleAttributes) throws RuntimeException {
        List<Transition> transitions = this.transitions.stream().toList();
        return graphImageGenerator.generateBufferedImage(
                transitions,
                format,
                styleAttributes.toArray(new StyleAttribute[0]));
    }

    private void imageGenerator(GraphImageGenerator graphImageGenerator, Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        List<Transition> transitions = this.transitions.stream().toList();
        try {
            Path path = Paths.get(outputPath);
            graphImageGenerator.generateImage(
                    transitions,
                    path.toAbsolutePath().toString(), // Absolute path by default
                    format,
                    styleAttributes.toArray(new StyleAttribute[0]));
        } catch (InvalidPathException e) {
            log.warn("Invalid path: " + outputPath + " using default path");
            graphImageGenerator.generateImage(transitions, format);
        } catch (IOException e) {
            log.error("Error generating workflow image: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a new builder for the DefaultStateWorkflow class.
     *
     * @param <T> the type of the stateful bean
     * @return a new builder for the DefaultStateWorkflow class
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // Module Attributes
    @Override
    public String graphName() {
        return name.toLowerCase();
    }

    @Override
    public List<String> labels() {
        if (this.module) {
            return List.of("Module");
        }
        return List.of("Workflow");
    }

    @Override
    public Object input() {
        return this.startNode.input();
    }

    @Override
    public Object output() {
        ComputedTransition lastComputedTransition = getComputedTransitions().stream()
                .reduce((first, second) -> second)
                .orElse(null);
        if (lastComputedTransition != null) {
            return lastComputedTransition.getPayload();
        }
        return null;
    }

    /**
     * A builder for the DefaultStateWorkflow class.
     *
     * @param <T> the type of the stateful bean
     */
    public static class Builder<T> {
        private String name;
        private T statefulBean;
        private List<Transition> addEdges = new ArrayList<>();
        private List<Node<T, ?>> addNodes = new ArrayList<>();
        private GraphImageGenerator graphImageGenerator;

        /**
         * Sets the name of the workflow.
         *
         * @param name the name of the workflow
         * @return this builder
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Constructs a new builder with the specified stateful bean.
         *
         * @param statefulBean the stateful bean to use in the workflow
         */
        public Builder<T> statefulBean(T statefulBean) {
            this.statefulBean = statefulBean;
            return this;
        }

        /**
         * Adds the specified edges to the workflow.
         *
         * @param edges the edges to add to the workflow
         * @return this builder
         */
        public Builder<T> addEdges(Transition... edges) {
            this.addEdges.addAll(Arrays.asList(edges));
            return this;
        }

        /**
         * Adds the specified nodes to the workflow.
         *
         * @param nodes the nodes to add to the workflow
         * @return this builder
         */
        public Builder<T> addNodes(Node<T, ?>... nodes) {
            this.addNodes.addAll(Arrays.asList(nodes));
            return this;
        }

        /**
         * Adds the specified graph image generator to the workflow.
         *
         * @param graphImageGenerator the graph image generator to add to the workflow
         * @return this builder
         */
        public Builder<T> graphImageGenerator(GraphImageGenerator graphImageGenerator) {
            this.graphImageGenerator = graphImageGenerator;
            return this;
        }

        /**
         * Builds a new DefaultStateWorkflow instance with the specified stateful bean.
         *
         * @param startNode the starting node of the workflow
         * @return a new DefaultStateWorkflow instance
         */
        public DefaultStateWorkflow<T> build(Node<T,?> startNode) {
            return this.build().startNode(startNode);
        }

        /**
         * Builds a new DefaultStateWorkflow instance with the specified stateful bean.
         *
         * @return a new DefaultStateWorkflow instance
         */
        public DefaultStateWorkflow<T> build() {
            return new DefaultStateWorkflow<>(this);
        }
    }

    @Data
    @AllArgsConstructor
    private static class CounterTransitionsPerState {
        private int inputTransitions;
        private int outputTransitions;

        public void incrementInputTransitions() {
            this.inputTransitions++;
        }

        public void incrementOutputTransitions() {
            this.outputTransitions++;
        }
    }
}
