package com.stockpilot.service;

import com.stockpilot.dto.PageResponse;
import com.stockpilot.dto.ProductDtos.ProductDetailResponse;
import com.stockpilot.dto.ProductDtos.ProductRequest;
import com.stockpilot.dto.ProductDtos.ProductResponse;
import com.stockpilot.dto.StockDtos.MovementResponse;
import com.stockpilot.dto.StockDtos.StockLevelResponse;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.StockLevel;
import com.stockpilot.exception.BadRequestException;
import com.stockpilot.exception.BusinessRuleException;
import com.stockpilot.exception.ConflictException;
import com.stockpilot.exception.NotFoundException;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.ProductTotal;
import com.stockpilot.repository.StockLevelRepository;
import com.stockpilot.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Services return DTOs, not entities. The mapping happens INSIDE the transaction,
 * while lazy relationships can still be loaded (we set open-in-view: false, so
 * nothing lazy can load once we're back in the controller).
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;

    // Whitelist: clients may only sort by these fields (not by anything unindexed or internal).
    private static final Set<String> SORTABLE = Set.of("name", "sku", "category", "unitPrice", "reorderLevel", "createdAt");

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, String category, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Can't sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
            }
        });
        String term = search == null ? "" : search.trim();
        String cat = (category == null || category.isBlank()) ? null : category.trim();
        Page<Product> page = productRepository.search(term, cat, pageable);

        // Avoid N+1: get the totals for all products on this page in ONE grouped query,
        // instead of one SUM query per product.
        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> totals = ids.isEmpty() ? Map.of()
                : stockLevelRepository.sumQuantityByProductIds(ids).stream()
                .collect(Collectors.toMap(ProductTotal::getProductId, ProductTotal::getTotal));

        return PageResponse.from(page, p -> ProductResponse.from(p, totals.getOrDefault(p.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(Long id) {
        Product product = findProduct(id);
        List<StockLevel> levels = stockLevelRepository.findByProductIdWithWarehouse(id);
        long total = levels.stream().mapToLong(StockLevel::getQuantity).sum();
        return new ProductDetailResponse(
                ProductResponse.from(product, total),
                levels.stream().map(StockLevelResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> movements(Long productId, Pageable pageable) {
        findProduct(productId); // 404 if the product doesn't exist
        return PageResponse.from(movementRepository.findByProductId(productId, pageable), MovementResponse::from);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(sku)) {
            throw new ConflictException("A product with SKU " + sku + " already exists.");
        }
        Product p = new Product();
        apply(p, request, sku);
        return ProductResponse.from(productRepository.save(p), 0);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = findProduct(id);
        String sku = request.sku().trim().toUpperCase();
        if (!p.getSku().equals(sku) && productRepository.existsBySku(sku)) {
            throw new ConflictException("A product with SKU " + sku + " already exists.");
        }
        apply(p, request, sku);
        // No save() call needed: p is a "managed" entity inside this transaction.
        // Hibernate's dirty checking notices the changed fields and issues the UPDATE on commit.
        long total = stockLevelRepository.findByProductIdWithWarehouse(id).stream()
                .mapToLong(StockLevel::getQuantity).sum();
        return ProductResponse.from(p, total);
    }

    @Transactional
    public void delete(Long id) {
        Product p = findProduct(id);
        // The ledger must stay complete, so a product with history can't be deleted.
        if (movementRepository.existsByProductId(id)) {
            throw new BusinessRuleException("This product has stock history and can't be deleted.");
        }
        productRepository.delete(p);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found."));
    }

    private static void apply(Product p, ProductRequest r, String sku) {
        p.setSku(sku);
        p.setName(r.name().trim());
        p.setCategory(r.category().trim());
        p.setUnitPrice(r.unitPrice());
        p.setReorderLevel(r.reorderLevel());
    }
}
