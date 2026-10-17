package com.stockpilot.dto;

import com.stockpilot.entity.MovementType;
import com.stockpilot.entity.StockLevel;
import com.stockpilot.entity.StockMovement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class StockDtos {

    private StockDtos() {
    }

    /** Only IN and OUT are allowed from this endpoint. Transfers and PO receipts have their own flows. */
    public enum AdjustmentType { IN, OUT }

    public record AdjustStockRequest(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @NotNull AdjustmentType type,
            @Positive int quantity,
            @Size(max = 255) String reason
    ) {
    }

    public record StockLevelResponse(
            Long warehouseId,
            String warehouseName,
            String city,
            int quantity,
            Long version
    ) {
        public static StockLevelResponse from(StockLevel s) {
            return new StockLevelResponse(s.getWarehouse().getId(), s.getWarehouse().getName(),
                    s.getWarehouse().getCity(), s.getQuantity(), s.getVersion());
        }
    }

    public record MovementResponse(
            Long id,
            MovementType type,
            int quantity,
            Long warehouseId,
            String warehouseName,
            String reason,
            String createdBy,
            Instant createdAt
    ) {
        public static MovementResponse from(StockMovement m) {
            return new MovementResponse(m.getId(), m.getType(), m.getQuantity(),
                    m.getWarehouse().getId(), m.getWarehouse().getName(),
                    m.getReason(), m.getCreatedBy(), m.getCreatedAt());
        }
    }
}
