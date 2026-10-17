package com.stockpilot.dto;

import com.stockpilot.dto.StockDtos.StockLevelResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class TransferDtos {

    private TransferDtos() {
    }

    public record TransferRequest(
            @NotNull Long productId,
            @NotNull Long fromWarehouseId,
            @NotNull Long toWarehouseId,
            @Positive int quantity,
            @Size(max = 255) String reason
    ) {
    }

    /** Both sides after the transfer, so the UI can update without another request. */
    public record TransferResponse(StockLevelResponse from, StockLevelResponse to) {
    }
}
