package com.stockpilot.repository;

import com.stockpilot.entity.PurchaseOrder;
import com.stockpilot.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Supplier and warehouse are fetched in the same query (no N+1).
    // Items are a collection, so they're NOT fetched here: join-fetching a collection breaks
    // SQL pagination. They load in batches instead (hibernate.default_batch_fetch_size).
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    // A single order: safe to fetch everything at once.
    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    Optional<PurchaseOrder> findWithDetailsById(Long id);

    long countByStatusIn(Collection<PurchaseOrderStatus> statuses);
}
