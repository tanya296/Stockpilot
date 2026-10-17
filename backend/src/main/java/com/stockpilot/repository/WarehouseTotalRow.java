package com.stockpilot.repository;

/** Projection for "total units per warehouse". */
public interface WarehouseTotalRow {
    String getWarehouseName();

    Long getTotalUnits();
}
