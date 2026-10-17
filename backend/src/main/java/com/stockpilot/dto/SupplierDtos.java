package com.stockpilot.dto;

import com.stockpilot.entity.Supplier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String contact
    ) {
    }

    public record SupplierResponse(Long id, String name, String contact) {
        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getContact());
        }
    }
}
