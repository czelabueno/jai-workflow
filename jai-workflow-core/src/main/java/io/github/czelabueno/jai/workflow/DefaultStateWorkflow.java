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
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public class DefaultStateWorkflow<T> implements StateWorkflow<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultStateWorkflow.class);
    private final String name;
    private final Map<TransitionState, List<TransitionState>> adjList;
    private volatile TransitionState startNode;
    private final T statefulBean;
    private final List<Transition> transitions; // definition transitions
    private final Map<TransitionState, CounterTransitionsPerState> transitionsPerState;
    private final List<TransitionState> compiledStates;
    private volatile int executionOrder;
    private final List<ComputedTransition> computedTransitions; // computed transitions after running
    private boolean module;
    private boolean wasRun;
    private List<String> labels;
    private final GraphImageGenerator graphImageGenerator;

    protected DefaultStateWorkflow(Builder<T> builder) {
        if (builder.statefulBean == null) {
            throw new IllegalArgumentException("Stateful bean cannot be null");
        }
        if (builder.addEdges == null || builder.addEdges.isEmpty()) {
            throw new IllegalArgumentException("At least one edged must be added to the workflow");
        }

        this.name = builder.name != null ? builder.name : "jAI-workflow";
        this.statefulBean = builder.statefulBean;
        this.adjList = new ConcurrentHashMap<>();
        this.transitions = Collections.synchronizedList(new ArrayList<>());
        this.computedTransitions = Collections.synchronizedList(new ArrayList<>());
        this.module = builder.asModule != null ? builder.asModule : false;

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

        if (state instanceof Node || state instanceof DefaultStateWorkflow) {
            isMerge = state.hasLabel("Merge");
            isParallel = state.hasLabel("Parallel");
            if (isParallel && isSplit) {
                throw new IllegalArgumentException("A parallel state '" + state.graphName() + "' cannot be a split state in the same flow");
            }
            if (isMerge && isSplit) {
                throw new IllegalArgumentException("A merge state '" + state.graphName() + "' cannot be a split state in the same flow");
            }
            if (isMerge && isParallel) {
                throw new IllegalArgumentException("A merge node '" + state.graphName() + "' cannot be a parallel state in the same flow");
            }
            if (isMerge) {
                int mergeInputTransitions = this.transitionsPerState.get(state).getInputTransitions(); // number of input transitions for merge node
                this.compiledStates.stream()
                        .filter(compiledState -> compiledState.hasLabel("Split"))
                        .findAny()
                        .ifPresent(existingSplitNode -> {
                            int splitOutputTransitions = this.transitionsPerState.get(existingSplitNode).getOutputTransitions(); // number of output transitions for split node
                            if (mergeInputTransitions != splitOutputTransitions) {
                                throw new IllegalArgumentException("The merge state '" + state.graphName() + "' must have the same number of input transitions as the number of output transitions from the split node '" + existingSplitNode.graphName() + "'");
                            }
                        });
            }
            if (isSplit) state.setLabels("Split");
        }
        this.compiledStates.add(state); // state compiled and validated

        // Determine startNode and setting it if necessary
        if (state == WorkflowStateName.START) {
            this.startNode = determineStartNode(this.startNode, this.adjList.get(state));
        }

        List<TransitionState> nextStates = this.adjList.get(state);
        for (TransitionState nextState : nextStates) {
            if (!this.compiledStates.contains(nextState)) {
                if (nextState instanceof Node || nextState instanceof DefaultStateWorkflow) {
                    if (isSplit || isParallel) {
                        nextState.setLabels("Parallel");
                    }
                    if (this.transitionsPerState.get(nextState).getInputTransitions() > 1 &&
                            this.transitions.stream()
                                .filter(transition -> transition.to().equals(nextState) && transition.from() instanceof Conditional)
                                .findAny()
                                .isEmpty() && // 'from' should not be a Conditional node
                            state.hasLabel("Parallel")){ //TODO: To check with other test cases
                        nextState.labels().clear();
                        nextState.setLabels("Merge");
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
    public DefaultStateWorkflow startNode(TransitionState startNode){
        this.startNode = startNode;
        return this;
    }

    /**
     * Returns the starting node defined in the workflow.
     *
     * @return the starting node defined in the workflow
     */
    @Override
    public TransitionState getStartNode() {
        return this.startNode;
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
        return run(this, null);
    }

    @Override
    public T runStream(Consumer<TransitionState> eventConsumer) {
        return run(this, eventConsumer);
    }

    private T run(DefaultStateWorkflow<T> workflow, Consumer<TransitionState> eventConsumer) {
        List<TransitionState> compiledStates = workflow.compiledStates;
        List<Transition> transitions = workflow.transitions;
        TransitionState startState = workflow.startNode;

        if (compiledStates == null || compiledStates.isEmpty()) {
            throw new IllegalStateException(workflow.name + " cannot run without a built workflow");
        }
        if (transitions == null || transitions.isEmpty()) {
            throw new IllegalStateException(workflow.name + "cannot run without transitions defined");
        }
        TransitionState startNode  = determineStartNode(startState, null);
        resetWorkflowState(workflow);
        log.debug("STARTING " + workflow.graphName() + (workflow.module ? " as a module":"") + "{}..", eventConsumer != null ? " in stream mode" : "");
        runState(workflow, startNode, eventConsumer);
        log.debug("END " + workflow.graphName() + (workflow.module ? " as a module":""));
        return statefulBean;
    }

    private TransitionState determineStartNode(TransitionState start, List<TransitionState> startNodes) {
        if (start == null) {
            if (startNodes.size() > 1) {
                throw new IllegalStateException("Its not possible to determine the start node, multiple start nodes found: " +
                        startNodes.stream().sorted(Comparator.comparing(startNodes::indexOf)).map(TransitionState::graphName).toList() +
                        "\nPlease specify the start node using the .startNode(Node<T> node) method");
            } else if (startNodes.size() == 1) {
                start = startNodes.get(0);
            }
        } else if (start instanceof DefaultStateWorkflow module) {
            if (module.startNode == null) {
                throw new IllegalArgumentException("The provided module " + module.name + " does not have a start node defined. Please specify the start node using the .startNode(Node<T> node) method");
            }
        }
        return start;
    }

    private void resetWorkflowState(DefaultStateWorkflow<T> workflow) {
        workflow.computedTransitions.clear(); // clean previous transitions
        workflow.executionOrder=1;
        workflow.wasRun = true; // mark workflow as run
    }

    private void runState(DefaultStateWorkflow<T> workflow, TransitionState state, Consumer<TransitionState> eventConsumer) {
        log.debug("Running state name: " + state.graphName() + "..");
        Map<TransitionState, List<TransitionState>> adjList = workflow.adjList;
        synchronized (this.statefulBean) {
            if (state instanceof Node node) {
                node.execute(this.statefulBean);
            } else if (state instanceof DefaultStateWorkflow module) {
                runModule(module, eventConsumer);
            }
        }
        if (eventConsumer != null) {
            eventConsumer.accept(state);
        }
        List<TransitionState> nextStates;
        synchronized (adjList) {
            nextStates = adjList.get(state);
        }

        if (state.hasLabel("Split")) {
            // Parallel execution
            processNextStatesInParallel(workflow, state, nextStates, eventConsumer);
            // Wait for all parallel tasks to continue with the Merge node
            List<ComputedTransition> lastParallelTransitions = workflow.getComputedTransitions().stream()
                    .filter(ct -> ct.getTransition().to().hasLabel("Parallel"))
                    .collect(groupingBy(ComputedTransition::getOrder))
                    .entrySet().stream()
                    .max(comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .orElse(emptyList());

            TransitionState mergeState = lastParallelTransitions.stream()
                    .flatMap(ct -> workflow.getTransitions().stream()
                            .filter(t -> t.from().equals(ct.getTransition().to()) && t.to().hasLabel("Merge")))
                    .map(Transition::to)
                    .findFirst()
                    .map(mergeTo -> { // compute all input transitions of the merge state before return
                        workflow.getTransitions().stream()
                                .filter(t -> t.to().hasLabel("Merge") && t.to().equals(mergeTo))
                                .distinct()
                                .forEach(t -> computeTransition(workflow, t.from(), t.to()));
                        return mergeTo;
                    })
                    .orElseThrow(() -> new RuntimeException("Merge state was not found. " +
                            "This should not happen if the workflow is correctly defined with a Merge node after a Parallel node"));
            // Run the Merge node after all parallel tasks are completed
            runState(workflow, mergeState, eventConsumer);
        } else { // Merge and others for this way
            processNextStatesSequentially(workflow, state, nextStates, eventConsumer);
        }
    }

    private void processNextStatesInParallel(DefaultStateWorkflow<T> workflow, TransitionState currentState, List<TransitionState> nextStates, Consumer<TransitionState> eventConsumer) {
        processNextStates(workflow, currentState, nextStates, true, eventConsumer);
    }

    private void processNextStatesSequentially(DefaultStateWorkflow<T> workflow, TransitionState currentState, List<TransitionState> nextStates, Consumer<TransitionState> eventConsumer) {
        processNextStates(workflow, currentState, nextStates, false, eventConsumer);
    }

    private void processNextStates(DefaultStateWorkflow<T> workflow, TransitionState currentState, List<TransitionState> nextStates, Boolean isParallel, Consumer<TransitionState> eventConsumer) {
        Stream<TransitionState> nextStatesStream = nextStates.stream();
        if (isParallel) {
            nextStatesStream = nextStatesStream.parallel();
            log.debug("Processing next states in parallel..");
        } else {
            log.debug("Processing next states sequentially..");
        }
        nextStatesStream.forEach((nextNode) -> {
            if (nextNode instanceof WorkflowStateName next) {
                if (next == WorkflowStateName.END) {
                    log.debug(workflow.graphName()+ " reached END state");
                    computeTransition(workflow, currentState, next);
                }
            } else if (nextNode instanceof Node next) {
                if (!next.hasLabel("Merge")) {
                    computeTransition(workflow, currentState, next);
                    runState(workflow, next, eventConsumer);
                }
            } else if (nextNode instanceof Conditional next) {
                computeTransition(workflow, currentState, next);
                TransitionState conditionalResult = next.evaluate(this.statefulBean);
                if (conditionalResult == null) {
                    throw new IllegalStateException("Conditional node returned null");
                } else if (conditionalResult instanceof Node conditionalNode) {
                    computeTransition(workflow, next, conditionalNode);
                    runState(workflow, conditionalNode, eventConsumer);
                } else if (conditionalResult instanceof DefaultStateWorkflow nextModule) {
                    if (!next.hasLabel("Merge")) {
                        computeTransition(workflow, next, nextModule);
                        runState(workflow, nextModule, eventConsumer);
                    }
//                    runModule(nextModule, eventConsumer);
                    // Resuming the next steps of the parent workflow
//                    List<TransitionState> nextStatesFromModule = workflow.adjList.get(nextModule);
//                    if (nextStatesFromModule != null && !nextStatesFromModule.isEmpty()) {
//                        processNextStates(workflow, nextModule, nextStatesFromModule, false, eventConsumer);
//                    }
                }
            } else if (nextNode instanceof DefaultStateWorkflow next) {
                if (!next.hasLabel("Merge")) {
                    computeTransition(workflow, currentState, next);
                    runState(workflow, next, eventConsumer);
                }
//                runModule(next, eventConsumer);
                // Resuming the next steps of the parent workflow
//                List<TransitionState> nextStatesFromModule = workflow.adjList.get(next);
//                if (nextStatesFromModule != null && !nextStatesFromModule.isEmpty()) {
//                    processNextStates(workflow, next, nextStatesFromModule, false, eventConsumer);
//                }
            }
        });
    }

    private void runModule(DefaultStateWorkflow<T> module, Consumer<TransitionState> eventConsumer) {
        if (!module.module) {
            throw new IllegalArgumentException("Workflow was not marked as module. It cannot be added to a running workflow");
        }
        run(module, eventConsumer);
    }

    private void computeTransition(DefaultStateWorkflow<T> workflow, TransitionState from, TransitionState to) {
        workflow.getTransitions().stream()
                .filter(transition -> transition.from().equals(from) && transition.to().equals(to))
                .findAny()
                .ifPresent(transition -> {
                    synchronized (workflow.getComputedTransitions()) {
                        Optional<Integer> existingOrder = workflow.getComputedTransitions().stream()
                                .filter(t -> t.getTransition().from().equals(from) || t.getTransition().to().equals(to))
                                .map(ComputedTransition::getOrder)
                                .findFirst();

                        int order = existingOrder.isPresent() ? existingOrder.get() : workflow.executionOrder++;
                        workflow.computedTransitions.add(ComputedTransition.from(order, transition));
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

    @Override
    public Boolean isModule() {
        return this.module;
    }

    /**
     * Convert a StateWorkflow as a Module.
     *
     * @return a StateWorkflow as a Module
     */
    @Override
    public DefaultStateWorkflow toModule() {
        this.module=true;
        return this;
    }

    /**
     * Returns true if the workflow has been run.
     *
     * @return true if the workflow has been run, false otherwise
     */
    @Override
    public Boolean wasRun() {
        return this.wasRun;
    }

    @Override
    public void generateComputedWorkflowImage(Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        GraphvizImageGenerator graphImageGenerator = GraphvizImageGenerator.builder().build();
        imageGenerator(graphImageGenerator, format, outputPath, styleAttributes);
    }

    @Override
    public BufferedImage generateComputedWorkflowBufferedImage(Format format, List<StyleAttribute> styleAttributes) throws RuntimeException {
        GraphvizImageGenerator graphImageGenerator = GraphvizImageGenerator.builder().build();
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
        return graphImageGenerator.generateBufferedImage(
                this,
                format,
                styleAttributes.toArray(new StyleAttribute[0]));
    }

    private void imageGenerator(GraphImageGenerator graphImageGenerator, Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        try {
            Path path = Paths.get(outputPath);
            graphImageGenerator.generateImage(
                    this,
                    path.toAbsolutePath().toString(), // Absolute path by default
                    format,
                    styleAttributes.toArray(new StyleAttribute[0]));
        } catch (InvalidPathException e) {
            log.warn("Invalid path: " + outputPath + " using default path");
            graphImageGenerator.generateImage(this, format);
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
        String label = "Workflow";
        if (this.module)
            label= "Module";
        if (labels == null) {
            return List.of(label);
        } else {
            labels.add(label);
        }
        return labels;
    }

    /**
     * Sets the labels of the state in the graph.
     *
     * @param labels the labels to set
     */
    @Override
    public void setLabels(String... labels) {
        if (this.labels == null) {
            this.labels = new ArrayList<>(Arrays.asList(labels));
        } else {
            this.labels.addAll(Arrays.asList(labels));
        }
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

        private Boolean asModule = false;
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
         * Adds the specified edges to the workflow.
         *
         * @param edges the edges to add to the workflow
         * @return this builder
         */
        public Builder<T> addEdges(List<Transition> edges) {
            this.addEdges.addAll(edges);
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
         * Adds the specified nodes to the workflow.
         *
         * @param nodes the nodes to add to the workflow
         * @return this builder
         */
        public Builder<T> addNodes(List<Node<T, ?>> nodes) {
            this.addNodes.addAll(nodes);
            return this;
        }

        public Builder<T> asModule() {
            this.asModule = true;
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
