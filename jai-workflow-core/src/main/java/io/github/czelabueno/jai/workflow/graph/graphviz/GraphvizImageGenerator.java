package io.github.czelabueno.jai.workflow.graph.graphviz;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.LinkTarget;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.Roughifyer;
import io.github.czelabueno.jai.workflow.StateWorkflow;
import io.github.czelabueno.jai.workflow.WorkflowStateName;
import io.github.czelabueno.jai.workflow.graph.Format;
import io.github.czelabueno.jai.workflow.graph.StyleAttribute;
import io.github.czelabueno.jai.workflow.node.Conditional;
import io.github.czelabueno.jai.workflow.node.Node;
import io.github.czelabueno.jai.workflow.transition.ComputedTransition;
import io.github.czelabueno.jai.workflow.transition.Transition;
import io.github.czelabueno.jai.workflow.graph.GraphImageGenerator;
import guru.nidi.graphviz.engine.*;
import io.github.czelabueno.jai.workflow.transition.TransitionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.nimbus.State;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;
import static guru.nidi.graphviz.model.Factory.node;
import static io.github.czelabueno.jai.workflow.graph.graphviz.Orientation.HORIZONTAL;
import static java.util.stream.Collectors.joining;

/**
 * Implementation of {@link GraphImageGenerator} that uses <a href="https://graphviz.org/">Graphviz</a> java library and DOT language to generate workflow images.
 */
public class GraphvizImageGenerator implements GraphImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(GraphvizImageGenerator.class);
    private static final Map<TransitionState, Boolean> onceComputedTransition = new HashMap<>();

    private String dotFormat;

    private GraphvizImageGenerator(GraphvizImageGeneratorBuilder builder) {
        this.dotFormat = builder.dotFormat;
    }

    /**
     * Returns a new builder instance for creating a {@link GraphvizImageGenerator}.
     *
     * @return a new {@link GraphvizImageGeneratorBuilder} instance
     */
    public static GraphvizImageGeneratorBuilder builder() {
        return new GraphvizImageGeneratorBuilder();
    }

    /**
     * Generates a graph image from the given list of transitions and saves it to the specified output path.
     *
     * @param workflow    the jAI workflow to generate the graph image from
     * @param outputPath  the path to save the generated graph image
     * @param format      the format of the generated image (e.g., SVG, PNG)
     * @param styles      optional styles to apply to the graph (e.g., sketchy, orientation)
     * @throws IOException if an I/O error occurs during image generation
     * @throws IllegalArgumentException if the output path is null or empty
     */
    @Override
    public void generateImage(StateWorkflow workflow , String outputPath, Format format, StyleAttribute... styles) throws IOException {
        if (outputPath == null || outputPath.isEmpty()) {
            throw new IllegalArgumentException("Output path can not be null or empty. Cannot generate image.");
        }
        log.debug("Saving workflow image..");
        createRenderer(workflow, format, styles).toFile(new File(outputPath));
        log.debug("Workflow image saved to: " + outputPath);
    }

    /**
     * Generates a BufferedImage representation of the workflow graph from the given list of transitions.
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @param format the format of the generated image (e.g., SVG, PNG)
     * @param styles optional styles to apply to the graph (e.g., sketchy, orientation)
     * @return the generated BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    @Override
    public BufferedImage generateBufferedImage(StateWorkflow workflow, Format format, StyleAttribute... styles) throws RuntimeException {
        log.debug("Generating workflow image..");
        Renderer renderer = createRenderer(workflow, format, styles);
        log.debug("Workflow image rendered to BufferedImage.");
        return renderer.toImage();
    }

    private Renderer createRenderer(StateWorkflow workflow, Format format, StyleAttribute... styles) {
        // Generate image using Graphviz from dot format
        final guru.nidi.graphviz.engine.Format IMAGE_FORMAT = graphvizFormatFrom(format);
        log.debug("Using default image format: " + IMAGE_FORMAT);

        boolean useDotFormat = dotFormat != null;
        Graphviz.useEngine(List.of(new GraphvizJdkEngine())); // Use GraalJS as the default engine

        Graphviz gv;
        if (useDotFormat) { // Custom dot format don't need transitions
            log.debug("Using custom Dot format: " + System.lineSeparator() + dotFormat);
            gv = Graphviz.fromString(dotFormat);
        } else {
            gv = Graphviz.fromGraph(createGraph(workflow, false, styles)); //JS engine is used by default
        }
        if (styles != null && styles.length > 0) {
            Graphviz finalGv = gv;
            gv = Arrays.stream(styles)
                    .filter(style -> style == StyleGraph.SKETCHY)
                    .findFirst()
                    .map(style -> finalGv.processor(new Roughifyer(new JdkJavascriptEngine()) // Use Roughifyer for sketchy style
                            .bowing(2)
                            .curveStepCount(6)
                            .roughness(1)
                            .fillStyle(FillStyle.hachure().width(2).gap(5).angle(0))
                            .font("*serif", "Comic Sans MS")))
                    .orElse(gv);
        }
        return gv.render(IMAGE_FORMAT);
    }

    /**
     * Builder class for {@link GraphvizImageGenerator}.
     */
    public static class GraphvizImageGeneratorBuilder {
        private String dotFormat;

        /**
         * Sets the dot format for the graph image.
         *
         * @param dotFormat the dot format string
         * @return the current {@link GraphvizImageGeneratorBuilder} instance
         */
        public GraphvizImageGeneratorBuilder dotFormat(String dotFormat) {
            this.dotFormat = dotFormat;
            return this;
        }

        /**
         * Builds and returns a new {@link GraphvizImageGenerator} instance.
         *
         * @return a new {@link GraphvizImageGenerator} instance
         */
        public GraphvizImageGenerator build() {
            return new GraphvizImageGenerator(this);
        }
    }

    /**
     * Generates the default DOT format string from the given list of transitions.
     * This method is deprecated, and it is recommended to use the {@link #createGraph(StateWorkflow, Boolean,StyleAttribute...)} method instead.
     *
     * @param transitions the list of transitions
     * @return the generated DOT format string
     * @deprecated Use {@link #createGraph(StateWorkflow, Boolean, StyleAttribute...)} for generating the graph representation.
     */
    @Deprecated
    private String defaultDotFormat(List<Transition> transitions) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph workflow {").append(System.lineSeparator());
        sb.append(" ").append("node [style=filled,fillcolor=lightgrey]").append(System.lineSeparator());
        sb.append(" ").append("rankdir=LR;").append(System.lineSeparator());
        sb.append(" ").append("beautify=true").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        for (Transition transition : transitions) {
            if (transition.to() instanceof Node) {
                sb.append(" ") // NodeFrom -> NodeTo
                        .append(transition.from() instanceof Node ?
                                sanitizeNodeName(((Node) transition.from()).getName()) :
                                transition.from().toString().toLowerCase())
                        .append(" -> ")
                        .append(sanitizeNodeName(((Node) transition.to()).getName())).append(";")
                        .append(System.lineSeparator());
            } else if (transition.to() == WorkflowStateName.END && transition.from() instanceof Node) {
                sb.append(" ") // NodeFrom -> END
                        .append(sanitizeNodeName(((Node) transition.from()).getName()))
                        .append(" -> ")
                        .append(((WorkflowStateName) transition.to()).toString().toLowerCase()).append(";")
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            } else {
                sb.append(" ") // NodeFrom -> NodeTo
                        .append(sanitizeNodeName(transition.from().toString().toLowerCase()))
                        .append(" -> ")
                        .append(sanitizeNodeName(transition.to().toString().toLowerCase())).append(";")
                        .append(System.lineSeparator());
            }
        }
        sb.append(" ")
                .append(WorkflowStateName.START.toString().toLowerCase()+" [shape=Mdiamond, fillcolor=\"orange\"];")
                .append(System.lineSeparator());
        sb.append(" ")
                .append(WorkflowStateName.END.toString().toLowerCase()+" [shape=Msquare, fillcolor=\"lightgreen\"];")
                .append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a graph representation from the given list of transitions.
     *
     * @param workflow the jAI workflow to generate the graph from
     * @param styles      optional styles to apply to the graph (e.g., orientation)
     * @return the generated graph representation
     */
    private static Graph createGraph(StateWorkflow workflow, Boolean isSubGraph, StyleAttribute... styles) {
        Rank.RankDir rankDir = Arrays.stream(styles)
                .filter(style -> style == HORIZONTAL)
                .findFirst()
                .map(style -> Rank.RankDir.LEFT_TO_RIGHT)
                .orElse(Rank.RankDir.TOP_TO_BOTTOM);

        List<Transition> transitions = workflow.getTransitions();
        if (transitions == null || transitions.isEmpty()) {
            throw new IllegalArgumentException("Transitions list can not be null or empty when dotFormat is null. Cannot generate image.");
        }

        boolean useComputedTransitions = workflow.wasRun();
        List<ComputedTransition> computedTransitions;
        if (useComputedTransitions) {
            computedTransitions = workflow.getComputedTransitions();
        } else {
            computedTransitions = Collections.emptyList();
        }

        onceComputedTransition.clear();

        Map<String, Graph> subGraphs = new HashMap<>();
        List<guru.nidi.graphviz.model.LinkSource> sources = transitions.stream()
                .map(transition -> {
                    if (transition.from() instanceof StateWorkflow wfFrom) {
                        subGraphs.computeIfAbsent(wfFrom.graphName(), key -> getGraphvizGraphFromModule(wfFrom, styles));
                    }
                    if (transition.to() instanceof StateWorkflow wfTo) {
                        subGraphs.computeIfAbsent(wfTo.graphName(), key -> getGraphvizGraphFromModule(wfTo, styles));
                    }
                    if (useComputedTransitions) {
                        boolean wasExecuted = computedTransitions.stream()
                                .anyMatch(computedTransition -> computedTransition.getTransition().equals(transition));
                        return createComputedGraphNode(transition, wasExecuted, isSubGraph); // Create a computed graph node
                    } else {
                        return createDefinitionGraphNode(transition, isSubGraph);
                    }
                })
                .filter(Objects::nonNull) // Filter out null nodes
                .collect(Collectors.toList());
        sources.addAll(subGraphs.values());

        // Count the occurrences of each node name
        Map<Label, Long> nodeNameCounts = sources.stream()
                .collect(Collectors.groupingBy(guru.nidi.graphviz.model.LinkSource::name, Collectors.counting()));

        // Style nodes that have multiple occurrences computed as executed
        List<guru.nidi.graphviz.model.LinkSource> newSources = sources.stream()
                .map(source -> {
                    if (nodeNameCounts.get(source.name()) > 1 && onceComputedTransition.keySet().stream()
                            .anyMatch(transitionState -> sanitizeNodeName(transitionState.graphName()).equals(source.name().value()))) {
                        if (source instanceof guru.nidi.graphviz.model.Node node) {
                            return node.with(Style.ROUNDED, Color.rgb(40, 167, 70), Color.rgb(91, 154, 119).fill());
                        } else if (source instanceof Graph graph){
                            return graph.graphAttr().with(Color.rgb(40, 167, 70));
                        }
                    }
                    return source;
                })
                .collect(Collectors.toUnmodifiableList());

        Graph graph = isSubGraph ?
                graph(sanitizeNodeName(workflow.graphName()))
                        .cluster()
                        .directed()
                        .graphAttr().with(
                                Color.rgb(110, 153, 104),
                                Label.of(sanitizeNodeName(workflow.graphName())),
                                Font.name("arial")):
                graph(sanitizeNodeName(workflow.graphName()))
                        .directed()
                        .graphAttr().with(
                                GraphAttr.splines(GraphAttr.SplineMode.POLYLINE),
                                GraphAttr.CENTER,
                                Rank.dir(rankDir));
        return graph
                .nodeAttr().with(Shape.RECTANGLE, Color.LIGHTBLUE2, Style.ROUNDED, Font.name("arial"))
                .linkAttr().with(Color.DARKGREEN, Style.SOLID, Arrow.NORMAL.size(0.8))
                .with(newSources);
    }

    /**
     * Creates a Graphviz node representation from the given transition.
     *
     * @param transition the transition to create the Graphviz node from
     * @return the created Graphviz node
     */
    private static guru.nidi.graphviz.model.LinkSource createComputedGraphNode (Transition transition, Boolean wasExecuted, Boolean isSubgraph) {
        if (transition.from() == WorkflowStateName.START) {
            return createDefinitionGraphNode(transition, isSubgraph);
        } else if (transition.to() == WorkflowStateName.END) {
            if (!isSubgraph) {
                Link linkToEnd = to(node(WorkflowStateName.END.graphName())
                        .with(Shape.M_SQUARE, Color.GREEN, Style.FILLED, Color.rgb(217, 255, 212).fill()));
                if (!wasExecuted) linkToEnd.add(Color.rgb(157, 165, 171));

                if (transition.from() instanceof StateWorkflow wfFrom) {
                    return styleNodeOnce(wfFrom.getLastNode(), wasExecuted).link(linkToEnd);
                }
                return styleNodeOnce(transition.from(), wasExecuted).link(linkToEnd);
            }
            return null;
        } else if (transition.from() instanceof Conditional) {
            guru.nidi.graphviz.model.Node conditionalNodeFrom = styleNodeOnce(transition.from(), wasExecuted);
            if (transition.to() instanceof StateWorkflow wfTo) {
                return conditionalNodeFrom.link(createLinkStyled(wfTo.getStartNode(), wasExecuted).add(Style.DASHED));
            }
            return conditionalNodeFrom.link(createLinkStyled(transition.to(), wasExecuted).add(Style.DASHED));
        } else if (transition.from() instanceof StateWorkflow wfFrom) {
            if (transition.to() instanceof StateWorkflow wfTo) {
                return styleNodeOnce(wfFrom.getLastNode(), wasExecuted).link(createLinkStyled(wfTo.getStartNode(), wasExecuted));
            } else if (transition.to() instanceof Node nodeTo) {
                return styleNodeOnce(wfFrom.getLastNode(), wasExecuted).link(createLinkStyled(nodeTo, wasExecuted));
            }
        } else if (transition.to() instanceof StateWorkflow wfTo) {
            if (transition.from() instanceof StateWorkflow wfFrom) {
                return styleNodeOnce(wfFrom.getLastNode(), wasExecuted).link(createLinkStyled(wfTo.getStartNode(), wasExecuted));
            } else if (transition.from() instanceof Node nodeFrom) {
                return styleNodeOnce(nodeFrom, wasExecuted).link(createLinkStyled(wfTo.getStartNode(), wasExecuted));
            }
        } else {
            return styleNodeOnce(transition.from(), wasExecuted)
                    .link(createLinkStyled(transition.to(), wasExecuted));
        }
        return null;
    }

    private static guru.nidi.graphviz.model.Node styleNodeOnce (TransitionState transitionState, Boolean wasExecuted) {
        if (onceComputedTransition.containsKey(transitionState) && onceComputedTransition.get(transitionState)) {
            return getGraphvizNodeFromNode(transitionState);
        }
        if (!onceComputedTransition.containsKey(transitionState) && wasExecuted) {
            onceComputedTransition.put(transitionState, true);
            return createNodeStyled(transitionState, true);
        }
        return createNodeStyled(transitionState, wasExecuted);
    }

    private static guru.nidi.graphviz.model.Node createNodeStyled (TransitionState transitionState, Boolean wasExecuted) {
        if (wasExecuted) {
            return getGraphvizNodeFromNode(transitionState)
                    .with(Color.rgb(40, 167, 70), Color.rgb(91, 154, 119).fill());
        } else {
            return getGraphvizNodeFromNode(transitionState)
                    .with(Color.rgb(157, 165, 171), Style.combine(Style.ROUNDED, Style.DASHED), Color.rgb(108, 117, 125).fill());
        }
    }

    private static Link createLinkStyled (TransitionState to, Boolean wasExecuted) {
        if (wasExecuted) {
            return to(getGraphvizNodeFromNode(to));
        } else {
            return to(getGraphvizNodeFromNode(to)).with(Color.rgb(157, 165, 171));
        }
    }

    private static guru.nidi.graphviz.model.LinkSource createDefinitionGraphNode (Transition transition, Boolean isSubGraph) {
        if (transition.from() == WorkflowStateName.START) {
            if (!isSubGraph){
                guru.nidi.graphviz.model.Node startNode = node(WorkflowStateName.START.graphName()).with(Shape.M_DIAMOND, Color.ORANGE, Style.FILLED, Color.rgb(255, 240, 212).fill());
                if (transition.to() instanceof StateWorkflow wfTo) {
                    return startNode.link(getGraphvizNodeFromNode(wfTo.getStartNode()));
                }
                return startNode.link(getGraphvizNodeFromNode(transition.to()));
            }
            return null;
        } else if (transition.to() == WorkflowStateName.END) {
            if (!isSubGraph) {
                guru.nidi.graphviz.model.Node endNode = node(WorkflowStateName.END.graphName())
                        .with(Shape.M_SQUARE, Color.GREEN, Style.FILLED, Color.rgb(217, 255, 212).fill());
                if (transition.from() instanceof StateWorkflow wfFrom) {
                    return getGraphvizNodeFromNode(wfFrom.getLastNode()).link(endNode);
                }
                return getGraphvizNodeFromNode(transition.from()).link(endNode);
            }
            return null;
        } else if (transition.from() instanceof Conditional) {
            guru.nidi.graphviz.model.Node conditionalNodeFrom = getGraphvizNodeFromNode(transition.from());
            if (transition.to() instanceof StateWorkflow wfTo) {
                return conditionalNodeFrom.link(to(getGraphvizNodeFromNode(wfTo.getStartNode())).with(Style.DASHED));
            }
            return conditionalNodeFrom.link(to(getGraphvizNodeFromNode(transition.to())).with(Style.DASHED));
        } else if (transition.from() instanceof StateWorkflow wfFrom) {
            if (transition.to() instanceof StateWorkflow wfTo) {
                return getGraphvizNodeFromNode(wfFrom.getLastNode()).link(getGraphvizNodeFromNode(wfTo.getStartNode()));
            } else if (transition.to() instanceof Node nodeTo) {
                return getGraphvizNodeFromNode(wfFrom.getLastNode()).link(getGraphvizNodeFromNode(nodeTo));
            }
        } else if (transition.to() instanceof StateWorkflow wfTo) {
            if (transition.from() instanceof StateWorkflow wfFrom) {
                return getGraphvizNodeFromNode(wfFrom.getLastNode()).link(getGraphvizNodeFromNode(wfTo.getStartNode()));
            } else if (transition.from() instanceof Node nodeFrom) {
                return getGraphvizNodeFromNode(nodeFrom).link(getGraphvizNodeFromNode(wfTo.getStartNode()));
            }
        } else {
            return getGraphvizNodeFromNode(transition.from()).link(getGraphvizNodeFromNode(transition.to()));
        }
        return null;
    }

    private static Graph getGraphvizGraphFromModule(StateWorkflow workflow, StyleAttribute... styles) {
        return createGraph(
                workflow,
                true,
                styles
        );
    }

    /**
     * Creates a Graphviz node representation from the given transition state (Node, Conditional or WorkflowStateName).
     *
     * @param transitionState the transition state to create the Graphviz node from
     * @return the created Graphviz node
     */
    private static guru.nidi.graphviz.model.Node getGraphvizNodeFromNode(TransitionState transitionState) {
        if (transitionState instanceof Node node) {
            List<String> labels = node.labels();
            String l = "";
            if (labels != null && !labels.isEmpty()) {
                l = labels.stream().map(label -> "<b><i>"+label.toLowerCase()+"</i></b><br/>").collect(joining());
            }
            Label label = Label.html(l.concat(sanitizeNodeName(node.graphName())));
            if (node.hasLabel("Split") || node.hasLabel("Merge")) {
                return node(sanitizeNodeName(node.graphName())).with(label, Color.ORANGE); // style with labels
            }
            return node(sanitizeNodeName(node.graphName())).with(label);
        } else if (transitionState instanceof Conditional node) {
            Label label = Label.html(node.graphName());
            return node(sanitizeNodeName(transitionState.graphName())).with(label,Shape.DIAMOND, Font.size(10));
        }
        return node(sanitizeNodeName(transitionState.graphName()));
    }

    /**
     * Sanitizes the node name by removing special characters and converting it to Java naming convention.
     *
     * @param nodeName the node name to sanitize
     * @return the sanitized node name
     */
    private static String sanitizeNodeName(String nodeName) {
        // Convert to camel case following Java naming convention
        return Arrays.stream(nodeName.replaceAll("[^a-zA-Z0-9 ]", "").split(" "))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(joining())
                .replaceFirst("^[A-Z]", nodeName.substring(0, 1).toLowerCase());
    }

    private static guru.nidi.graphviz.engine.Format graphvizFormatFrom(Format format) {
        switch (format) {
            case SVG:
                return guru.nidi.graphviz.engine.Format.SVG;
            case PNG:
                return guru.nidi.graphviz.engine.Format.PNG;
            default:
                return guru.nidi.graphviz.engine.Format.SVG;
        }
    }
}
