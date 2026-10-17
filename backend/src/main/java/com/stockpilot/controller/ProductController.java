package com.stockpilot.controller;

import com.stockpilot.dto.PageResponse;
import com.stockpilot.dto.ProductDtos.ProductDetailResponse;
import com.stockpilot.dto.ProductDtos.ProductRequest;
import com.stockpilot.dto.ProductDtos.ProductResponse;
import com.stockpilot.dto.StockDtos.MovementResponse;
import com.stockpilot.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products?search=brake&category=Brakes&page=0&size=10&sort=name,asc
     * Spring builds the Pageable from page, size and sort automatically.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) String category,
                                              @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return productService.search(search, category, pageable);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    @GetMapping("/{id}")
    public ProductDetailResponse get(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    @GetMapping("/{id}/movements")
    public PageResponse<MovementResponse> movements(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.movements(id, pageable);
    }

    // Only ADMINs manage the product catalogue. The check happens on the server, before the method runs.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
