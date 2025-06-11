package io.github.czelabueno.jai.workflow.graph;

import io.github.czelabueno.jai.workflow.StateWorkflow;
import io.github.czelabueno.jai.workflow.graph.graphviz.Orientation;
import io.github.czelabueno.jai.workflow.graph.graphviz.StyleGraph;
import io.github.czelabueno.jai.workflow.transition.Transition;

import javax.swing.plaf.nimbus.State;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Interface for generating graph images from workflow transitions computed.
 */
public interface GraphImageGenerator {

    /**
     * Generates a graph image from the given list of transitions and saves it to the default output path: /workflow-image.svg
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @throws IOException if an I/O error occurs during image generation
     */
    default void generateImage(StateWorkflow workflow) throws IOException {
        generateImage(workflow, "workflow-image.svg");
    }

    /**
     * Generates a graph image with the given format from the given list of transitions and saves it to the default output path.
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @param format the format of the generated image
     * @throws IOException if an I/O error occurs during image generation
     */
    default void generateImage(StateWorkflow workflow, Format format) throws IOException {
        generateImage(workflow, "workflow-image." + format.name().toLowerCase(), format);
    }

    /**
     * Generates a graph image from the given list of transitions and saves it to the given output path.
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @param outputPath the path to save the generated graph image
     * @param format the format of the generated image
     * @param styles the styles to apply to the generated image
     * @throws IOException if an I/O error occurs during image generation
     */
    void generateImage(StateWorkflow workflow, String outputPath, Format format, StyleAttribute... styles) throws IOException;

    /**
     * Generates a graph image with the given format from the given list of transitions and saves it to the specified output path.
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @param outputPath  the path to save the generated graph image
     * @param format the format of the generated image
     * @throws IOException if an I/O error occurs during image generation
     * @throws IllegalArgumentException if the output path is null or empty
     */
    default void generateImage(StateWorkflow workflow, String outputPath, Format format) throws IOException {
        generateImage(workflow, outputPath, format, StyleGraph.DEFAULT, Orientation.VERTICAL);
    }

    /**
     * Generates a graph image with {@link Format}.SVG from the given list of transitions and saves it to the specified output path.
     *
     * @param workflow the jAI workflow to generate the graph image from
     * @param outputPath  the path to save the generated graph image
     * @throws IOException if an I/O error occurs during image generation
     * @throws IllegalArgumentException if the output path is null or empty
     */
    default void generateImage(StateWorkflow workflow, String outputPath) throws IOException {
        generateImage(workflow, outputPath, Format.SVG);
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
    BufferedImage generateBufferedImage(StateWorkflow workflow, Format format, StyleAttribute... styles) throws RuntimeException;

    /**
     * Generates a BufferedImage representation of the workflow graph from the given list of transitions.
     * @param workflow the jAI workflow to generate the graph image from
     * @param styles optional styles to apply to the graph (e.g., sketchy, orientation)
     * @return the generated BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateBufferedImage(StateWorkflow workflow, StyleAttribute... styles) throws RuntimeException {
        return generateBufferedImage(workflow, Format.SVG, styles);
    }

    /**
     * Generates a BufferedImage representation of the workflow graph from the given list of transitions.
     * @param workflow the jAI workflow to generate the graph image from
     * @param format the format of the generated image (e.g., SVG, PNG)
     * @return the generated BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateBufferedImage(StateWorkflow workflow, Format format) throws RuntimeException {
        return generateBufferedImage(workflow, format, StyleGraph.DEFAULT, Orientation.VERTICAL);
    }

    /**
     * Generates a BufferedImage representation of the workflow graph from the given list of transitions.
     * @param workflow the jAI workflow to generate the graph image from
     * @return the generated BufferedImage representation of the workflow graph
     * @throws RuntimeException if an error occurs during image generation
     */
    default BufferedImage generateBufferedImage(StateWorkflow workflow) throws RuntimeException {
        return generateBufferedImage(workflow, Format.SVG);
    }
}
