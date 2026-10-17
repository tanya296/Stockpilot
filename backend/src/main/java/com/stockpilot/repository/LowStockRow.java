package com.stockpilot.repository;

/** Projection for the low-stock query. */
public interface LowStockRow {
    Long getProductId();

    String getSku();

    String getName();

    Integer getReorderLevel();

    Long getTotal();
}
