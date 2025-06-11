package io.github.czelabueno.jai.workflow.transition;

import java.util.List;

/**
 * Represents a component in the jAI workflow anatomy that can produce a transition and update the workflow state.
 * <p>
 * For example, a {@link io.github.czelabueno.jai.workflow.node.Node} can produce a transition by executing a function.
 * </p>
 * <p>
 * Classes implementing this interface can be used to generate a state as part of a {@link Transition}.
 * </p>
 */
public interface TransitionState{

    /**
     * Returns the name of the state in the graph.
     *
     * @return the name of the state in the graph
     */
    String graphName();

    /**
     * Returns the labels of the state in the graph.
     *
     * @return the labels of the state in the graph
     */
    List<String> labels();

    /**
     * Sets the labels of the state in the graph.
     *
     * @param labels the labels to set
     */
    void setLabels(String... labels);

    /**
     * Checks if the state has a given label.
     *
     * @param label the label to check
     * @return true if the state has the given label, false otherwise
     */
    default boolean hasLabel(String label){
        if (labels() == null) {
            return false;
        }
        return labels().contains(label);
    }
    /**
     * Returns the input of the state.
     *
     * @return the input of the state
     */
    Object input();

    /**
     * Returns the output of the state.
     *
     * @return the output of the state
     */
    Object output();
}
