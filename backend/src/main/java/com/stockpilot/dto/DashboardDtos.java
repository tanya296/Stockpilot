package com.stockpilot.dto;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record WarehouseTotal(String warehouseName, long totalUnits) {
    }

    public record LowStockItem(
            Long productId,
            String sku,
            String name,
            long totalQuantity,
            int reorderLevel,
            long shortfall
    ) {
    }

    public record DashboardSummary(
            long totalProducts,
            long lowStockCount,
            long openPurchaseOrders,
            List<WarehouseTotal> stockByWarehouse
    ) {
    }
}
