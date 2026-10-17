package com.stockpilot.entity;

import java.util.Map;
import java.util.Set;

/**
 * A small state machine. Each status lists the statuses it may move to.
 * DRAFT -> ORDERED -> RECEIVED, and DRAFT/ORDERED -> CANCELLED.
 * RECEIVED and CANCELLED are final.
 */
public enum PurchaseOrderStatus {
    DRAFT, ORDERED, RECEIVED, CANCELLED;

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(ORDERED, CANCELLED),
            ORDERED, Set.of(RECEIVED, CANCELLED),
            RECEIVED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canMoveTo(PurchaseOrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
