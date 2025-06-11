package io.github.czelabueno.jai.workflow.node;

import io.github.czelabueno.jai.workflow.transition.TransitionState;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * Represents a Node in a workflow that executes a function with a given input and produces an output.
 * <p>
 * A Node represents a single unit of work within the workflow. It encapsulates a specific function or task that processes the stateful bean and updates it.
 * </p>
 * <p>
 * This class implements the {@link TransitionState} interface.
 * </p>
 * @param <T> the type of the input to the function. Usually a stateful bean POJO defined by the user.
 * @param <R> the type of the output from the function. Usually a stateful bean POJO defined by the user.
 */
public class Node<T, R> implements TransitionState {

    @Getter
    private final String name;
    private final Function<T, R> function;
    private List<String> labels;
    private T functionInput;
    private R functionOutput;

    /**
     * Constructs a Node with the specified name and function.
     *
     * @param name     the name of the node
     * @param function the function to execute
     * @throws IllegalArgumentException if the node name is empty
     * @throws NullPointerException     if the name or function is null
     */
    public Node(@NonNull String name, @NonNull Function<T, R> function) {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Node name cannot be empty");
        }
        this.name = name;
        this.function = function;
    }

    /**
     * Adds the specified labels to the Node.
     *
     * @param labels the labels to add
     */
    @Override
    public void setLabels(String... labels) {
        if (this.labels == null) {
            this.labels = new ArrayList<>(Arrays.asList(labels));
        } else {
            this.labels.addAll(Arrays.asList(labels));
        }
    }

    /**
     * Gets the label for the Node.
     *
     * @param label the label to get
     * @return the label for the Node
     */
    public String getLabel(String label) {
        if (labels == null) {
            return null;
        }
        return labels.stream()
                .filter(l -> l.equals(label))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the Node has the specified label.
     *
     * @param label the label to check
     * @return true if the Node has the label, false otherwise
     */
    public boolean hasLabel(String label) {
        if (labels == null) {
            return false;
        }
        return labels.contains(label);
    }

    /**
     * Executes the function with the given input and stores the input and output.
     *
     * @param input the input to the function
     * @return the output from the function
     * @throws IllegalArgumentException if the input is null
     */
    public R execute(T input) {
        if (input == null) {
            throw new IllegalArgumentException("Function input cannot be null");
        }
        functionInput = input;
        R output = function.apply(input);
        functionOutput = output;
        return output;
    }

    /**
     * Creates a new Node with the specified name and function.
     *
     * @param name     the name of the node
     * @param function the function to execute
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the output from the function
     * @return a new Node instance
     */
    public static <T, R> Node<T, R> from(String name, Function<T, R> function) {
        return new Node<>(name, function);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node<?, ?> node = (Node<?, ?>) o;

        if (!Objects.equals(name, node.name)) return false;
        return Objects.equals(function, node.function);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (function != null ? function.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", function=" + function +
                '}';
    }

    /**
     * Returns the name formatted for the graph.
     *
     * @return the name formatted for the graph.
     */
    @Override
    public String graphName() {
        return name.toLowerCase();
    }

    /**
     * Returns the labels for a Node.
     *
     * @return the labels for a Node.
     */
    @Override
    public List<String> labels() {
        return labels != null ? labels : emptyList();
    }

    @Override
    public Object input() {
        return functionInput;
    }

    @Override
    public Object output() {
        return functionOutput;
    }
}
