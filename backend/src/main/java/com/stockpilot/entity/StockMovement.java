package com.stockpilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One line in the append-only stock ledger.
 * No setters on purpose: once written, a movement is never changed.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", updatable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MovementType type;

    // Always positive. The type says whether stock went up or down.
    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(updatable = false)
    private String reason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockMovement(Product product, Warehouse warehouse, MovementType type,
                         int quantity, String reason, String createdBy) {
        this.product = product;
        this.warehouse = warehouse;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }
}
