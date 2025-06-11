package io.github.czelabueno.jai.workflow.node;

import io.github.czelabueno.jai.workflow.transition.TransitionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

class ConditionalTest {

    // validated
    Node<Integer, String> node1;
    Node<Integer, String> node2;

    @BeforeEach
    void setUp() {
        Function<Integer, String> sumToString = num -> {
            num += 1;
            return num.toString();
        };
        Function<Integer, String> subsToString = num -> {
            num -= 1;
            return num.toString();
        };
        node1 = Node.from("node1", sumToString);
        node2 = Node.from("node2", subsToString);
    }
    @Test
    void test_validate_constructor() {
        Function<String, Node> condition = s -> s.equals("sum") ? node1 : node2;
        Conditional conditional = new Conditional("condition", condition, List.of(node1,node2));
        assertThat(conditional).isNotNull();
    }

    @Test
    void test_null_condition_constructor() {
        // Condition null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Conditional("condition", null, List.of(node1)))
                .withMessage("Condition function cannot be null");
        // Expected nodes null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Conditional("condition", s -> s, null))
                .withMessage("The list of nodes expected from the condition function cannot be null");
    }

    @Test
    void test_empty_expected_nodes_constructor() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Conditional("condition", s -> s, List.of()))
                .withMessage("The list of nodes expected from the condition function cannot be empty");
    }

    @Test
    void test_evaluate_valid_input() {
        // given
        Function<String, Node> condition = s -> s.equals("sum") ? node1 : node2;

        // when
        Conditional conditional = new Conditional("condition", condition, List.of(node1, node2));
        TransitionState node = conditional.evaluate("sum");

        // then
        assertThat(node).isNotNull();
        assertThat(node).isEqualTo(node1);
        assertThat(node.graphName()).isEqualTo("node1");

        // when
        TransitionState other = conditional.evaluate("other");

        // then
        assertThat(other).isNotNull();
        assertThat(other).isEqualTo(node2);
        assertThat(other.graphName()).isEqualTo("node2");
    }

    @Test
    void test_evaluate_null_input() {
        Function<String, Node> condition = s -> s.equals("subs") ? node2 : node1;

        Conditional conditional = new Conditional("condition", condition, List.of(node2, node1));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> conditional.evaluate(null))
                .withMessage("Function Input cannot be null");
    }

    @Test
    void test_inline_eval_static_method() {
        // given
        TransitionState nodeReturned = Conditional.<Integer>eval(
                "condition",
                s -> s > 5 ? node2 : node1, // function input type expected is the same node input type
                List.of(node2, node1)
        ).evaluate(6);

        // then
        assertThat(nodeReturned).isNotNull();
        assertThat(nodeReturned).isEqualTo(node2);
        assertThat(nodeReturned.graphName()).isEqualTo("node2");
    }

    @Test
    void test_ignore_generic_type_in_eval_static_method() {
        // given
        Function condition = s -> s.equals("subs") ? node2 : node1; // function without generic type

        Conditional conditional = Conditional.eval("condition",
                condition,
                List.of(node2, node1));
        TransitionState node = conditional.evaluate("sum");

        // then
        assertThat(node).isNotNull();
        assertThat(node).isEqualTo(node1);
        assertThat(node.graphName()).isEqualTo("node1");

        // when
        TransitionState other = conditional.evaluate("subs");

        // then
        assertThat(other).isNotNull();
        assertThat(other).isEqualTo(node2);
        assertThat(other.graphName()).isEqualTo("node2");
    }

    @Test
    void test_any_expected_nodes_does_not_match_with_returned_node() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Conditional.<Integer>eval(
                        "condition",
                        s -> s > 5 ? node2 : node1,
                        List.of(node1) // node2 is expected
                        )
                        .evaluate(6))
                .withMessageStartingWith("The condition function returned an invalid node type")
                .withMessageContaining("node1")
                .withMessageEndingWith("but got: node2 instead.");
    }

    @Test
    void test_conditional_get_graph_name() {
        Function<String, Node> condition = s -> node1;
        Conditional conditional = new Conditional("CONDITION", condition, List.of(node1));
        assertThat(conditional.graphName()).isEqualTo("condition"); // lower case expected
    }

    @Test
    void test_conditional_has_labels() {
        Function<String, Node> condition = s -> node1;
        Conditional conditional = new Conditional("condition", condition, List.of(node1));
        assertThat(conditional.hasLabel("label")).isFalse();
        assertThat(conditional.hasLabel("Conditional")).isTrue();
        assertThat(conditional.labels()).anySatisfy(label -> assertThat(label).isEqualTo("Conditional"));
    }

    @Test
    void test_conditional_input_and_output_values() {
        Function<String, Node> condition = s -> node1;
        Conditional conditional = new Conditional("condition", condition, List.of(node1));
        assertThat(conditional.input()).isNull(); // null because the .evaluate() method is not called
        assertThat(conditional.output()).isNull();

        conditional.evaluate("input");
        assertThat(conditional.input()).isEqualTo("input");
        assertThat(conditional.output()).isEqualTo(node1.getName()); // output value contains the node name only
    }

    @Test
    void test_conditional_input_and_output_types_and_values() {
        Function<Integer, Node> condition = s -> s > 5 ? node2 : node1;
        Conditional conditional = new Conditional("condition", condition, List.of(node1, node2));
        conditional.evaluate(6);

        // Check input type and value
        assertThat(conditional.input()).isInstanceOf(Integer.class);
        assertThat(conditional.input()).isEqualTo(6);
        // Check output type and value
        assertThat(conditional.output()).isInstanceOf(String.class);
        assertThat(conditional.output()).isEqualTo(node2.getName());
    }

    @Test
    void test_equals_and_hash() {
        Function<String, Node> condition1 = s -> node1;
        Function<String, Node> condition2 = s -> node2;
        Conditional conditional1 = new Conditional("condition", condition1, List.of(node1));
        Conditional conditional2 = new Conditional("condition", condition1, List.of(node1));
        Conditional conditional3 = new Conditional("condition2", condition2, List.of(node2));
        assertThat(conditional1)
                .isEqualTo(conditional1)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object())
                .isEqualTo(conditional2)
                .isNotEqualTo(conditional3)
                .hasSameHashCodeAs(conditional2);
    }

    @Test
    void test_toString() {
        Function<String, Node> condition = s -> node1;
        Conditional conditional = new Conditional("condition", condition, List.of(node1));
        assertThat(conditional.toString()).isEqualTo("Conditional{name='condition, condition=" + condition + ", validNodes=" + List.of(node1) + "}");
    }
}
