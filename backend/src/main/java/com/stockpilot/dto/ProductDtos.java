package com.stockpilot.dto;

import com.stockpilot.entity.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {
    }

    /** What an ADMIN sends to create or update a product. */
    public record ProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 100) String category,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
            @PositiveOrZero int reorderLevel
    ) {
    }

    /** One row in the products table on the frontend. */
    public record ProductResponse(
            Long id,
            String sku,
            String name,
            String category,
            BigDecimal unitPrice,
            int reorderLevel,
            long totalQuantity,
            boolean lowStock
    ) {
        public static ProductResponse from(Product p, long totalQuantity) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getCategory(),
                    p.getUnitPrice(), p.getReorderLevel(), totalQuantity,
                    totalQuantity < p.getReorderLevel());
        }
    }

    /** The product detail page: the product plus its stock in each warehouse. */
    public record ProductDetailResponse(
            ProductResponse product,
            List<StockDtos.StockLevelResponse> stock
    ) {
    }
}
