package com.smartbatch360.api.order;

import java.util.Set;

/**
 * Order lifecycle state.
 *
 * Only UNFULFILLED existed at first (user scope decision 2026-08-27, which
 * deliberately deferred "the rest of the order lifecycle"); the transitions
 * were added 2026-08-28.
 *
 * Unlike Batch's controls - which are deliberately permissive because they
 * stand in for hardware that isn't wired up yet - these transitions ARE
 * enforced. An order's state is a business record, not a simulated signal, so
 * moving a fulfilled order back to in-progress is a data error rather than an
 * operator shortcut.
 */
public enum OrderStatus {

    /** Created, nothing produced against it yet. */
    UNFULFILLED,

    /** Production has started against the order. */
    IN_PROGRESS,

    /** Fully delivered. Terminal. */
    FULFILLED,

    /** Abandoned before completion. Terminal. */
    CANCELLED;

    private static final Set<OrderStatus> TERMINAL = Set.of(FULFILLED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Whether this order may move to {@code target}. */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case UNFULFILLED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == FULFILLED || target == CANCELLED;
            case FULFILLED, CANCELLED -> false;
        };
    }
}
