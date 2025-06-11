package io.github.czelabueno.jai.workflow.node;

import io.github.czelabueno.jai.workflow.transition.TransitionState;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a conditional node in a workflow that evaluates a condition function to transition a new jAI workflow state.
 * <p>
 * Implements the {@link TransitionState} interface.
 *
 * @param <T> the stateful bean POJO defined by the user. It is used to store the state of the workflow.
 */
public class Conditional<T> implements TransitionState {

    private final String name;
    private Function<T, TransitionState> condition;
    @Getter
    private final List<TransitionState> expectedNodes;
    private T functionInput;
    private TransitionState functionOutput;
    private List<String> labels;

    /**
     * Constructs a Conditional with the specified condition function and valid node types list.
     *
     * @param name the name of the conditional node
     * @param condition the condition function to evaluate
     * @param expectedNodes the list of nodes expected from the condition function
     * @throws NullPointerException if the condition function or expected node list is null
     * @throws IllegalArgumentException if the expected node list is empty or the condition function's return type is not valid
     */
    public Conditional(String name, @NonNull Function<T, TransitionState> condition, @NonNull List<TransitionState> expectedNodes) {
        this.condition = Objects.requireNonNull(condition, "Condition function cannot be null");
        this.expectedNodes = Objects.requireNonNull(expectedNodes, "The list of nodes expected from the condition function cannot be null");
        if (expectedNodes.isEmpty()) {
            throw new IllegalArgumentException("The list of nodes expected from the condition function cannot be empty");
        }
        this.name= name;
    }

    /**
     * Evaluates the condition function with the given stateful bean.
     *
     * @param input the stateful bean as input to the condition function
     * @return the resulting Node from the condition function
     * @throws NullPointerException if the input is null
     */
    public TransitionState evaluate(T input) {
        Objects.requireNonNull(input, "Function Input cannot be null");
        functionInput = input;
        condition = condition.andThen(resultNode -> {
            if (!expectedNodes.contains(resultNode)) {
                throw new RuntimeException("The condition function returned an invalid node type. Expected one of: " + expectedNodes + " but got: " + resultNode.graphName() + " instead.");
            }
            return resultNode;
        });
        functionOutput = condition.apply(input);
        return functionOutput;
    }

    /**
     * Creates a new Conditional with the specified condition function.
     *
     * @param name the name of the conditional node
     * @param condition the condition function to evaluate
     * @param expectedNodes the list of nodes expected from the condition function
     * @param <T> the stateful bean as input to the condition function
     * @return a new Conditional instance
     */
    public static <T> Conditional<T> eval(String name, Function<T, TransitionState> condition, List<TransitionState> expectedNodes) {
        return new Conditional<>(name, condition, expectedNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Conditional<?> that = (Conditional<?>) o;

        return Objects.equals(condition, that.condition) &&
                Objects.equals(expectedNodes, that.expectedNodes) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = condition != null ? condition.hashCode() : 0;
        result = 31 * result + (expectedNodes != null ? expectedNodes.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Conditional{" +
                "name='" + name +
                ", condition=" + condition +
                ", validNodes=" + expectedNodes +
                '}';
    }

    /**
     * Returns the name formatted for the graph.
     *
     * @return the name formatted for the graph.
     */
    @Override
    public String graphName() {
        if (name != null) {
            return name.toLowerCase();
        }
        return "Conditional".toLowerCase();
    }

    /**
     * Returns the labels for {@link Conditional}.
     *
     * @return the labels for a Conditional Node.
     */
    @Override
    public List<String> labels() {
        String label = "Conditional";
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
        return functionInput;
    }

    @Override
    public Object output() {
        if (functionOutput == null) {
            return null;
        }
        return functionOutput.graphName(); // Node name
    }
}
