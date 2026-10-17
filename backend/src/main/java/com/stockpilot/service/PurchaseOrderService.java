package com.stockpilot.service;

import com.stockpilot.dto.PageResponse;
import com.stockpilot.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.stockpilot.dto.PurchaseOrderDtos.ItemRequest;
import com.stockpilot.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.BadRequestException;
import com.stockpilot.exception.BusinessRuleException;
import com.stockpilot.exception.NotFoundException;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.PurchaseOrderRepository;
import com.stockpilot.repository.SupplierRepository;
import com.stockpilot.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> list(PurchaseOrderStatus status, Pageable pageable) {
        var page = status == null
                ? purchaseOrderRepository.findAllBy(pageable)
                : purchaseOrderRepository.findByStatus(status, pageable);
        return PageResponse.from(page, PurchaseOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(Long id) {
        return PurchaseOrderResponse.from(find(id));
    }

    /** Creates an order in DRAFT. Nothing happens to stock until it is RECEIVED. */
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, String userEmail) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier " + request.supplierId() + " not found."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse " + request.warehouseId() + " not found."));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setWarehouse(warehouse);
        po.setCreatedBy(userEmail);

        Set<Long> seen = new HashSet<>();
        for (ItemRequest itemRequest : request.items()) {
            if (!seen.add(itemRequest.productId())) {
                throw new BadRequestException("Product " + itemRequest.productId() + " appears twice; combine the lines.");
            }
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException("Product " + itemRequest.productId() + " not found."));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.unitCost());
            po.addItem(item);
        }

        // CascadeType.ALL on items: saving the order also inserts every item.
        return PurchaseOrderResponse.from(purchaseOrderRepository.save(po));
    }

    /**
     * Moves the order through its state machine.
     * RECEIVED adds every item to the warehouse's stock (with ledger rows) in the SAME transaction
     * as the status change: either the status changes and all stock arrives, or nothing happens.
     * @Version on PurchaseOrder stops two managers receiving the same order twice: the second
     * one's UPDATE finds a stale version, fails, and its whole transaction (stock included) rolls back.
     */
    @Transactional
    public PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus next, String userEmail) {
        PurchaseOrder po = find(id);
        PurchaseOrderStatus current = po.getStatus();

        if (!current.canMoveTo(next)) {
            throw new BusinessRuleException("Can't move a purchase order from " + current + " to " + next + ".");
        }

        if (next == PurchaseOrderStatus.RECEIVED) {
            for (PurchaseOrderItem item : po.getItems()) {
                stockService.changeStock(item.getProduct(), po.getWarehouse(), MovementType.PO_RECEIPT,
                        item.getQuantity(), "PO #" + po.getId() + " from " + po.getSupplier().getName(), userEmail);
            }
        }

        po.setStatus(next);
        po.setUpdatedAt(Instant.now());
        purchaseOrderRepository.flush(); // surface any version conflict now
        return PurchaseOrderResponse.from(po);
    }

    private PurchaseOrder find(Long id) {
        return purchaseOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order " + id + " not found."));
    }
}
