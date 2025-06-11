package io.github.czelabueno.jai.workflow;

import io.github.czelabueno.jai.workflow.transition.TransitionState;

import java.util.List;

/**
 * Enum representing the possible states in a workflow.
 * <p>
 * This class implements the {@link TransitionState} interface.
 */
public enum WorkflowStateName implements TransitionState {
    /**
     * The starting state of the workflow.
     */
    START("_start_"),

    /**
     * The ending state of the workflow.
     */
    END("_end_");

    private final String graphName;

    WorkflowStateName(String graphName) {
        this.graphName = graphName;
    }

    @Override
    public String graphName() {
        return graphName;
    }

    @Override
    public List<String> labels() {
        return List.of(graphName);
    }

    /**
     * Sets the labels of the state in the graph.
     *
     * @param labels the labels to set
     */
    @Override
    public void setLabels(String... labels) {
        // No labels to set for WorkflowStateName
        throw new UnsupportedOperationException("WorkflowStateName does not support setting labels.");
    }

    @Override
    public Object input() {
        return Void.TYPE;
    }

    @Override
    public Object output() {
        return Void.TYPE;
    }
}
