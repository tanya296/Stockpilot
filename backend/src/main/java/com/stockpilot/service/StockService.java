package com.stockpilot.service;

import com.stockpilot.dto.StockDtos.AdjustStockRequest;
import com.stockpilot.dto.StockDtos.AdjustmentType;
import com.stockpilot.dto.StockDtos.StockLevelResponse;
import com.stockpilot.dto.TransferDtos.TransferRequest;
import com.stockpilot.dto.TransferDtos.TransferResponse;
import com.stockpilot.exception.BadRequestException;
import com.stockpilot.entity.*;
import com.stockpilot.exception.BusinessRuleException;
import com.stockpilot.exception.NotFoundException;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockLevelRepository;
import com.stockpilot.repository.StockMovementRepository;
import com.stockpilot.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only class allowed to change stock. Every change goes through changeStock(),
 * which enforces "never negative" and always writes a ledger row.
 * Transfers and purchase-order receipts (Tier 2) will reuse it.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;

    /**
     * @Transactional: everything inside runs in ONE database transaction.
     * Either the stock update AND the movement row are both saved, or neither is.
     */
    @Transactional
    public StockLevelResponse adjust(AdjustStockRequest request, String userEmail) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " not found."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse " + request.warehouseId() + " not found."));

        MovementType type = request.type() == AdjustmentType.IN ? MovementType.IN : MovementType.OUT;
        StockLevel level = changeStock(product, warehouse, type, request.quantity(), request.reason(), userEmail);

        // flush() sends the UPDATE now (instead of at commit), so the response
        // shows the new version number, and any optimistic-lock conflict surfaces here.
        stockLevelRepository.flush();
        return StockLevelResponse.from(level);
    }

    /**
     * Moves stock from one warehouse to another, atomically.
     * Step 1 (take from source) and step 2 (add to destination) plus both ledger rows run in ONE
     * transaction. If anything fails (not enough stock, a version conflict, the DB going down),
     * Spring rolls the whole thing back: stock can never vanish or be created out of thin air.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request, String userEmail) {
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new BadRequestException("Source and destination warehouse must be different.");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " not found."));
        Warehouse from = warehouseRepository.findById(request.fromWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse " + request.fromWarehouseId() + " not found."));
        Warehouse to = warehouseRepository.findById(request.toWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse " + request.toWarehouseId() + " not found."));

        String reason = request.reason() != null && !request.reason().isBlank()
                ? request.reason()
                : "Transfer " + from.getName() + " -> " + to.getName();

        StockLevel source = changeStock(product, from, MovementType.TRANSFER_OUT, request.quantity(), reason, userEmail);
        StockLevel destination = changeStock(product, to, MovementType.TRANSFER_IN, request.quantity(), reason, userEmail);

        stockLevelRepository.flush();
        return new TransferResponse(StockLevelResponse.from(source), StockLevelResponse.from(destination));
    }

    /**
     * Applies one stock change and records it in the ledger.
     * Must be called inside a transaction (all public callers are @Transactional).
     */
    StockLevel changeStock(Product product, Warehouse warehouse, MovementType type,
                           int quantity, String reason, String userEmail) {
        boolean decrease = type == MovementType.OUT || type == MovementType.TRANSFER_OUT;

        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> new StockLevel(product, warehouse)); // first time this product is in this warehouse

        if (decrease && level.getQuantity() < quantity) {
            throw new BusinessRuleException("Not enough stock of " + product.getSku() + " in "
                    + warehouse.getName() + ": " + level.getQuantity() + " available, " + quantity + " requested.");
        }

        level.setQuantity(decrease ? level.getQuantity() - quantity : level.getQuantity() + quantity);
        stockLevelRepository.save(level);
        movementRepository.save(new StockMovement(product, warehouse, type, quantity, reason, userEmail));
        return level;
    }
}
