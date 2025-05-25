package io.github.czelabueno.jai.workflow.transition;

import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a computed transition in a jai workflow.
 */
@Getter
public class ComputedTransition{
    private final UUID id;
    private final Integer order;
    private final Transition transition;
    private final LocalDateTime computedAt;
    private final Object payload;
    private final Thread thread;

    private ComputedTransition(@NonNull Integer order, @NonNull Transition transition) {
        if (transition.from() == null) {
            throw new RuntimeException("Transition node 'from' cannot be null");
        }
        if (transition.to() == null) {
            throw new RuntimeException("Transition node 'to' cannot be null");
        }
        if (order <= 0) {
            throw new RuntimeException("Transition order cannot be negative");
        }
        this.id = UUID.randomUUID();
        this.order = order;
        this.transition = transition;
        this.computedAt = LocalDateTime.now();
        this.payload = transition.from().output();
        this.thread = Thread.currentThread();
    }

    /**
     * Creates a new ComputedTransition with the specified order and transition.
     *
     * @param order the order of the transition
     * @param transition the transition to compute
     * @return a new ComputedTransition instance
     */
    public static ComputedTransition from(@NonNull Integer order, @NonNull Transition transition) {
        return new ComputedTransition(order, transition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComputedTransition that = (ComputedTransition) o;

        if (!id.equals(that.id)) return false;
        if (!order.equals(that.order)) return false;
        if (!transition.equals(that.transition)) return false;
        if (!computedAt.equals(that.computedAt)) return false;
        return payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + order.hashCode();
        result = 31 * result + transition.hashCode();
        result = 31 * result + computedAt.hashCode();
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ComputedTransition{" +
                "id=" + id +
                ", order=" + order +
                ", transition=" + transition +
                ", computedAt=" + computedAt +
                ", payload=" + payload +
                ", thread=" + thread +
                '}';
    }
}
