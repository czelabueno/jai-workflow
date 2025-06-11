package io.github.czelabueno.jai.workflow;

import io.github.czelabueno.jai.workflow.graph.Format;
import io.github.czelabueno.jai.workflow.graph.StyleAttribute;
import io.github.czelabueno.jai.workflow.node.Conditional;
import io.github.czelabueno.jai.workflow.node.Node;
import io.github.czelabueno.jai.workflow.transition.ComputedTransition;
import io.github.czelabueno.jai.workflow.transition.Transition;
import io.github.czelabueno.jai.workflow.transition.TransitionState;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface representing a state workflow.
 *
 * @param <T> the type of the stateful bean used in the workflow
 */
public interface StateWorkflow<T> extends TransitionState {

    /**
     * Adds a node to the workflow.
     *
     * @param node the node to add
     */
    void addNode(Node<T, ?> node);

    /**
     * Creates an edge between two nodes in the workflow.
     *
     * @param from the starting node of the edge
     * @param to   the ending node of the edge
     */
    void putEdge(Node<T, ?> from, Node<T, ?> to);

    /**
     * Creates an edge between a node and a conditional node in the workflow.
     *
     * @param from        the starting node of the edge
     * @param conditional the conditional node to evaluate
     */
    void putEdge(Node<T, ?> from, Conditional<T> conditional);

    /**
     * Creates an edge between a node and a workflow state in the workflow.
     *
     * @param from  the starting node of the edge
     * @param state the workflow state to transition to
     */
    void putEdge(Node<T, ?> from, WorkflowStateName state);

    /**
     * Sets the starting node of the workflow.
     *
     * @param startNode the starting node
     * @return the state workflow with the starting node set
     */
    StateWorkflow startNode(TransitionState startNode);

    /**
     * Returns the starting node defined in the workflow.
     *
     * @return the starting node defined in the workflow
     */
    TransitionState getStartNode();

    /**
     * Returns the last node defined in the workflow.
     *
     * @return the last node defined in the workflow
     */
    Node<T,?> getLastNode();

    /**
     * Runs the workflow synchronously.
     *
     * @return the stateful bean after the workflow execution
     */
    T run();

    /**
     * Runs the workflow in stream mode, consuming events with the specified consumer.
     *
     * @param eventConsumer the consumer to process node events
     * @return the stateful bean after the workflow execution
     */
    T runStream(Consumer<TransitionState> eventConsumer);

    /**
     * Returns the list of definition transition in the workflow.
     *
     * @return the list of definition transitions
     */
    List<Transition> getTransitions();

    /**
     * Returns the list of computed transitions in the workflow.
     *
     * @return the list of computed transitions
     */
    List<ComputedTransition> getComputedTransitions();

    /**
     * Returns if the workflow was run or not.
     *
     * @return a boolean indicating if the workflow was run
     */
    Boolean wasRun();

    /**
     * Returns if workflow is a module or not
     *
     * @return a boolean indicating if the workflow is a module
     */
    Boolean isModule();

    /**
     * Convert a StateWorkflow as a Module.
     *
     * @return a StateWorkflow as a Module
     */
    StateWorkflow toModule();

    /**
     * Generates an image of the workflow and saves it to the specified output path.
     *
     * @param format the format of the image
     * @param outputPath the path to save the workflow image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @throws IOException if an I/O error occurs
     */
    void generateWorkflowImage(Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException;

    /**
     * Generates an image of the workflow and saves it to the specified output path.
     *
     * @param outputPath the path to save the workflow image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateWorkflowImage(String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        generateWorkflowImage(Format.SVG, outputPath, styleAttributes);
    }

    /**
     * Generates an image of the workflow and saves it to the specified output path.
     *
     * @param format the format of the image
     * @param outputPath the path to save the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateWorkflowImage(Format format, String outputPath) throws IOException {
        generateWorkflowImage(format, outputPath, List.of());
    }

    /**
     * Generates an image of the workflow and saves it to the specified output path.
     *
     * @param outputPath the path to save the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateWorkflowImage(String outputPath) throws IOException {
        generateWorkflowImage(Format.SVG, outputPath);
    }

    /**
     * Generates an image of the workflow and saves it to the default path "workflow-image.svg".
     *
     * @throws IOException if an I/O error occurs
     */
    default void generateWorkflowImage() throws IOException {
        generateWorkflowImage("workflow-image.svg");
    }

    /**
     * Generates a BufferedImage representation of the workflow graph.
     *
     * @param format the format of the image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @return the BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    BufferedImage generateWorkflowBufferedImage(Format format, List<StyleAttribute> styleAttributes) throws RuntimeException;

    /**
     * Generates a BufferedImage representation of the workflow graph with Format.SVG. by default.
     *
     * @param styleAttributes the style attributes to apply to the workflow image
     * @return the BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateWorkflowBufferedImage(List<StyleAttribute> styleAttributes) throws RuntimeException {
        return generateWorkflowBufferedImage(Format.SVG, styleAttributes);
    }

    /**
     * Generates a BufferedImage representation of the workflow graph with the specified format.
     *
     * @param format the format of the image
     * @return the BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateWorkflowBufferedImage(Format format) throws RuntimeException {
        return generateWorkflowBufferedImage(format, List.of());
    }

    /**
     * Generates a BufferedImage representation of the workflow graph with Format.SVG. by default.
     *
     * @return the BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateWorkflowBufferedImage() throws RuntimeException {
        return generateWorkflowBufferedImage(Format.SVG);
    }

    /**
     * Generates an image of the computed workflow and saves it to the specified output path.
     *
     * @param format the format of the image
     * @param outputPath the path to save the workflow image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @throws IOException if an I/O error occurs
     */
    void generateComputedWorkflowImage(Format format, String outputPath, List<StyleAttribute> styleAttributes) throws IOException;

    /**
     * Generates an image of the computed workflow and saves it to the specified output path with Format.SVG by default.
     *
     * @param outputPath the path to save the workflow image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateComputedWorkflowImage(String outputPath, List<StyleAttribute> styleAttributes) throws IOException {
        generateComputedWorkflowImage(Format.SVG, outputPath, styleAttributes);
    }

    /**
     * Generates an image of the computed workflow and saves it to the specified output path.
     *
     * @param format the format of the image
     * @param outputPath the path to save the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateComputedWorkflowImage(Format format, String outputPath) throws IOException {
        generateComputedWorkflowImage(format, outputPath, List.of());
    }

    /**
     * Generates an image of the computed workflow and saves it to the specified output path with Format.SVG by default.
     *
     * @param outputPath the path to save the workflow image
     * @throws IOException if an I/O error occurs
     */
    default void generateComputedWorkflowImage(String outputPath) throws IOException {
        generateComputedWorkflowImage(Format.SVG, outputPath);
    }

    /**
     * Generates an image of the computed workflow and saves it to the default path "computed-workflow-image.svg" in SVG format.
     *
     * @throws IOException if an I/O error occurs
     */
    default void generateComputedWorkflowImage() throws IOException {
        generateComputedWorkflowImage("computed-workflow-image.svg");
    }

    /**
     * Generates a BufferedImage representation of the computed workflow graph.
     *
     * @param format the format of the image
     * @param styleAttributes the style attributes to apply to the workflow image
     * @return the BufferedImage representation of the computed workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    BufferedImage generateComputedWorkflowBufferedImage(Format format, List<StyleAttribute> styleAttributes) throws RuntimeException;

    /**
     * Generates a BufferedImage representation of the computed workflow graph with Format.SVG by default.
     *
     * @param styleAttributes the style attributes to apply to the workflow image
     * @return the BufferedImage representation of the computed workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateComputedWorkflowBufferedImage(List<StyleAttribute> styleAttributes) throws RuntimeException {
        return generateComputedWorkflowBufferedImage(Format.SVG, styleAttributes);
    }

    /**
     * Generates a BufferedImage representation of the computed workflow graph with the specified format.
     *
     * @param format the format of the image
     * @return the BufferedImage representation of the computed workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateComputedWorkflowBufferedImage(Format format) throws RuntimeException {
        return generateComputedWorkflowBufferedImage(format, List.of());
    }

    /**
     * Generates a BufferedImage representation of the computed workflow graph with Format.SVG by default.
     *
     * @return the BufferedImage representation of the computed workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateComputedWorkflowBufferedImage() throws RuntimeException {
        return generateComputedWorkflowBufferedImage(Format.SVG);
    }
}
