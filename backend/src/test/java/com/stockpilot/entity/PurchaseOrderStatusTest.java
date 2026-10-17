package com.stockpilot.entity;

import org.junit.jupiter.api.Test;

import static com.stockpilot.entity.PurchaseOrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A plain unit test: no Spring, no database, runs in milliseconds.
 */
class PurchaseOrderStatusTest {

    @Test
    void allowsTheHappyPath() {
        assertThat(DRAFT.canMoveTo(ORDERED)).isTrue();
        assertThat(ORDERED.canMoveTo(RECEIVED)).isTrue();
    }

    @Test
    void allowsCancellingBeforeReceipt() {
        assertThat(DRAFT.canMoveTo(CANCELLED)).isTrue();
        assertThat(ORDERED.canMoveTo(CANCELLED)).isTrue();
    }

    @Test
    void finalStatesCannotChange() {
        for (PurchaseOrderStatus next : PurchaseOrderStatus.values()) {
            assertThat(RECEIVED.canMoveTo(next)).isFalse();
            assertThat(CANCELLED.canMoveTo(next)).isFalse();
        }
    }

    @Test
    void cannotSkipOrdering() {
        assertThat(DRAFT.canMoveTo(RECEIVED)).isFalse();
    }
}
