package com.stockpilot.repository;

import com.stockpilot.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    @Query("select distinct p.category from Product p order by p.category")
    java.util.List<String> findDistinctCategories();

    /**
     * JPQL works on entities and fields (Product, p.name), not tables and columns.
     * Pageable adds LIMIT/OFFSET and ORDER BY, and Spring runs a second COUNT query
     * so the Page knows the total number of results.
     * "search" is never null (the service passes "" for no search), which keeps
     * PostgreSQL from complaining that it can't infer the parameter's type.
     */
    @Query("""
            select p from Product p
            where (lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.sku) like lower(concat('%', :search, '%')))
              and (:category is null or p.category = :category)
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("category") String category,
                         Pageable pageable);
}
