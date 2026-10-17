package com.stockpilot.controller;

import com.stockpilot.dto.PageResponse;
import com.stockpilot.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.stockpilot.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.stockpilot.dto.PurchaseOrderDtos.StatusUpdateRequest;
import com.stockpilot.entity.PurchaseOrderStatus;
import com.stockpilot.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return purchaseOrderService.list(status, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return purchaseOrderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request, Authentication auth) {
        return purchaseOrderService.create(request, auth.getName());
    }

    /** PATCH because we change one field (status) of an existing resource. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrderResponse updateStatus(@PathVariable Long id,
                                              @Valid @RequestBody StatusUpdateRequest request,
                                              Authentication auth) {
        return purchaseOrderService.updateStatus(id, request.status(), auth.getName());
    }
}
