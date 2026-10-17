package com.stockpilot.service;

import com.stockpilot.dto.DashboardDtos.DashboardSummary;
import com.stockpilot.dto.DashboardDtos.LowStockItem;
import com.stockpilot.dto.DashboardDtos.WarehouseTotal;
import com.stockpilot.entity.PurchaseOrderStatus;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.PurchaseOrderRepository;
import com.stockpilot.repository.StockLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only aggregates. The database does the counting and summing (GROUP BY),
 * which is far faster than loading every row into Java.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        return stockLevelRepository.findLowStock().stream()
                .map(r -> new LowStockItem(r.getProductId(), r.getSku(), r.getName(), r.getTotal(),
                        r.getReorderLevel(), r.getReorderLevel() - r.getTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        List<WarehouseTotal> perWarehouse = stockLevelRepository.totalsByWarehouse().stream()
                .map(r -> new WarehouseTotal(r.getWarehouseName(), r.getTotalUnits()))
                .toList();
        return new DashboardSummary(
                productRepository.count(),
                stockLevelRepository.findLowStock().size(),
                purchaseOrderRepository.countByStatusIn(List.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.ORDERED)),
                perWarehouse);
    }
}
