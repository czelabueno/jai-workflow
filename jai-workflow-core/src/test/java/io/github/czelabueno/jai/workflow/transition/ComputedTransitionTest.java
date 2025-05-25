package io.github.czelabueno.jai.workflow.transition;

import io.github.czelabueno.jai.workflow.node.Node;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComputedTransitionTest {

    // validated
    @Test
    void should_build_computed_transition_using_from() {
        // given
        // node mocked to simulate that the transition was computed
        Node mockFromNode = mock(Node.class);
        Node mockToNode = mock(Node.class);
        when(mockFromNode.output()).thenReturn("mockedPayloadOfFromNode");
        when(mockFromNode.graphName()).thenReturn("from");
        when(mockToNode.graphName()).thenReturn("to");

        ComputedTransition computedTransition = ComputedTransition.from(
                1,
                Transition.from(mockFromNode, mockToNode)
        );

        // then
        assertThat(computedTransition.getId()).isNotNull();
        assertThat(computedTransition.getOrder()).isEqualTo(1);
        assertThat(computedTransition.getTransition().from().graphName()).isEqualTo("from");
        assertThat(computedTransition.getTransition().to().graphName()).isEqualTo("to");
        assertThat(computedTransition.getComputedAt()).isBefore(LocalDateTime.now());
        assertThat(computedTransition.getPayload()).isEqualTo("mockedPayloadOfFromNode");
    }

    @Test
    void should_throw_exception_when_build_order_arg_is_null() {
        // then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        null,
                        null
                ))
                .withMessage("order is marked non-null but is null");
    }

    @Test
    void should_throw_exception_when_build_transition_arg_is_null() {
        // then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        1,
                        null
                ))
                .withMessage("transition is marked non-null but is null");
    }

    @Test
    void should_throw_exception_when_transition_from_is_null() {
        // given
        Transition mockTransition = mock(Transition.class);
        when(mockTransition.from()).thenReturn(null);

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        1,
                        mockTransition
                ))
                .withMessage("Transition node 'from' cannot be null");
    }

    @Test
    void should_throw_exception_when_transition_to_is_null() {
        // given
        Node mockFromNode = mock(Node.class);
        Transition mockTransition = mock(Transition.class);
        when(mockTransition.from()).thenReturn(mockFromNode);
        when(mockTransition.to()).thenReturn(null);

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        1,
                        mockTransition
                ))
                .withMessage("Transition node 'to' cannot be null");
    }

    @Test
    void should_throw_exception_when_transition_order_is_negative() {
        // given
        Node mockFromNode = mock(Node.class);
        Node mockToNode = mock(Node.class);
        Transition mockTransition = mock(Transition.class);
        when(mockTransition.from()).thenReturn(mockFromNode);
        when(mockTransition.to()).thenReturn(mockToNode);

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        -1,
                        mockTransition
                ))
                .withMessage("Transition order cannot be negative");
    }

    @Test
    void should_throw_exception_when_transition_order_is_zero() {
        // given
        Node mockFromNode = mock(Node.class);
        Node mockToNode = mock(Node.class);
        Transition mockTransition = mock(Transition.class);
        when(mockTransition.from()).thenReturn(mockFromNode);
        when(mockTransition.to()).thenReturn(mockToNode);

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> ComputedTransition.from(
                        0,
                        mockTransition
                ))
                .withMessage("Transition order cannot be negative");
    }

    @Test
    void test_equals_and_hash() {
        // given
        Node mockFromNode = mock(Node.class);
        Node mockToNode = mock(Node.class);
        Transition transition = Transition.from(mockFromNode, mockToNode);
        ComputedTransition computedTransition1 = ComputedTransition.from(1, transition);
        ComputedTransition computedTransition2 = ComputedTransition.from(1, transition);

        // then
        // Each instance has a different id. They can never be the same.
        assertThat(computedTransition1)
                .isEqualTo(computedTransition1)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object())
                .isNotEqualTo(computedTransition2)
                .doesNotHaveSameHashCodeAs(computedTransition2);
    }

    @Test
    void test_toString() {
        // given
        Node mockFromNode = mock(Node.class);
        Node mockToNode = mock(Node.class);
        Transition transition = Transition.from(mockFromNode, mockToNode);
        ComputedTransition computedTransition = ComputedTransition.from(1, transition);

        // then
        assertThat(computedTransition.toString()).isEqualTo("ComputedTransition{id=" + computedTransition.getId() + ", order=1, transition=" + transition + ", computedAt=" + computedTransition.getComputedAt() + ", payload=" + computedTransition.getPayload() + ", thread=" + computedTransition.getThread() + "}");
    }
}
