package com.stockpilot.dto;

import com.stockpilot.entity.PurchaseOrder;
import com.stockpilot.entity.PurchaseOrderItem;
import com.stockpilot.entity.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PurchaseOrderDtos {

    private PurchaseOrderDtos() {
    }

    public record ItemRequest(
            @NotNull Long productId,
            @Positive int quantity,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitCost
    ) {
    }

    /** @Valid on the list makes Bean Validation check every item too. */
    public record CreatePurchaseOrderRequest(
            @NotNull Long supplierId,
            @NotNull Long warehouseId,
            @NotEmpty @Valid List<ItemRequest> items
    ) {
    }

    public record StatusUpdateRequest(@NotNull PurchaseOrderStatus status) {
    }

    public record ItemResponse(
            Long productId,
            String sku,
            String productName,
            int quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal
    ) {
        public static ItemResponse from(PurchaseOrderItem i) {
            return new ItemResponse(i.getProduct().getId(), i.getProduct().getSku(), i.getProduct().getName(),
                    i.getQuantity(), i.getUnitCost(), i.getUnitCost().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
    }

    public record PurchaseOrderResponse(
            Long id,
            Long supplierId,
            String supplierName,
            Long warehouseId,
            String warehouseName,
            PurchaseOrderStatus status,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            BigDecimal totalCost,
            List<ItemResponse> items
    ) {
        public static PurchaseOrderResponse from(PurchaseOrder po) {
            List<ItemResponse> items = po.getItems().stream().map(ItemResponse::from).toList();
            BigDecimal total = items.stream().map(ItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new PurchaseOrderResponse(po.getId(), po.getSupplier().getId(), po.getSupplier().getName(),
                    po.getWarehouse().getId(), po.getWarehouse().getName(), po.getStatus(), po.getCreatedBy(),
                    po.getCreatedAt(), po.getUpdatedAt(), total, items);
        }
    }
}
