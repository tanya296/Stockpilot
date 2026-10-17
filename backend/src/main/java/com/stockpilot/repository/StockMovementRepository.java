package com.stockpilot.repository;

import com.stockpilot.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // @EntityGraph is the annotation version of "join fetch": avoids N+1 on warehouse.
    @EntityGraph(attributePaths = "warehouse")
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductId(Long productId);
}
