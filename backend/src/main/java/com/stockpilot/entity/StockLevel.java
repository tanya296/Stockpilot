package com.stockpilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How many units of one product sit in one warehouse.
 */
@Entity
@Table(name = "stock_levels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: don't load the Product row until someone actually calls getProduct().
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    private int quantity;

    /**
     * Optimistic locking. Hibernate adds "AND version = ?" to every UPDATE and bumps it by one.
     * If another transaction changed the row first, 0 rows match and Hibernate throws
     * ObjectOptimisticLockingFailureException, which we turn into HTTP 409.
     */
    @Version
    private Long version;

    public StockLevel(Product product, Warehouse warehouse) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = 0;
    }
}
