package com.stockpilot.dto;

import com.stockpilot.entity.Warehouse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class WarehouseDtos {

    private WarehouseDtos() {
    }

    public record WarehouseRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String city
    ) {
    }

    public record WarehouseResponse(Long id, String name, String city) {
        public static WarehouseResponse from(Warehouse w) {
            return new WarehouseResponse(w.getId(), w.getName(), w.getCity());
        }
    }
}
