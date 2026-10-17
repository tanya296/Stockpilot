package com.stockpilot.controller;

import com.stockpilot.dto.StockDtos.AdjustStockRequest;
import com.stockpilot.dto.StockDtos.StockLevelResponse;
import com.stockpilot.dto.TransferDtos.TransferRequest;
import com.stockpilot.dto.TransferDtos.TransferResponse;
import com.stockpilot.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * Stock in or out at one warehouse. STAFF and above (the role hierarchy covers MANAGER and ADMIN).
     * Authentication is injected by Spring from the SecurityContext our JWT filter filled;
     * getName() is the email from the token, used as "createdBy" in the ledger.
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('STAFF')")
    public StockLevelResponse adjust(@Valid @RequestBody AdjustStockRequest request, Authentication auth) {
        return stockService.adjust(request, auth.getName());
    }

    /** Move stock between warehouses. MANAGER and above. */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('MANAGER')")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request, Authentication auth) {
        return stockService.transfer(request, auth.getName());
    }
}
