package com.stockpilot.repository;

import com.stockpilot.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    /**
     * "join fetch" loads each StockLevel AND its Warehouse in ONE query.
     * Without it we'd get the N+1 problem: 1 query for the stock levels,
     * then 1 more query per row the first time we touch getWarehouse().
     */
    /** Total quantity per product, for many products at once (GROUP BY). */
    @Query("select s.product.id as productId, sum(s.quantity) as total from StockLevel s where s.product.id in :ids group by s.product.id")
    List<ProductTotal> sumQuantityByProductIds(@Param("ids") List<Long> ids);

    /**
     * Products whose TOTAL stock across all warehouses is below their reorder level.
     * LEFT JOIN so a product with no stock rows at all (total 0) still shows up.
     */
    @Query("""
            select p.id as productId, p.sku as sku, p.name as name, p.reorderLevel as reorderLevel,
                   coalesce(sum(s.quantity), 0) as total
            from Product p left join StockLevel s on s.product = p
            group by p.id, p.sku, p.name, p.reorderLevel
            having coalesce(sum(s.quantity), 0) < p.reorderLevel
            order by p.name
            """)
    List<LowStockRow> findLowStock();

    @Query("""
            select w.name as warehouseName, coalesce(sum(s.quantity), 0) as totalUnits
            from Warehouse w left join StockLevel s on s.warehouse = w
            group by w.id, w.name
            order by w.name
            """)
    List<WarehouseTotalRow> totalsByWarehouse();

    @Query("select s from StockLevel s join fetch s.warehouse where s.product.id = :productId order by s.warehouse.name")
    List<StockLevel> findByProductIdWithWarehouse(@Param("productId") Long productId);
}
