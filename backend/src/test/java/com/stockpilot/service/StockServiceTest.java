package com.stockpilot.service;

import com.stockpilot.dto.StockDtos.AdjustStockRequest;
import com.stockpilot.dto.StockDtos.AdjustmentType;
import com.stockpilot.entity.*;
import com.stockpilot.exception.BusinessRuleException;
import com.stockpilot.exception.NotFoundException;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockLevelRepository;
import com.stockpilot.repository.StockMovementRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test of the service with Mockito.
 * The repositories are fakes (@Mock): no database. We control what they return and
 * verify what the service did with them. This tests the business rules in isolation.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock StockLevelRepository stockLevelRepository;
    @Mock StockMovementRepository movementRepository;

    @InjectMocks StockService stockService; // gets the four mocks through its constructor

    Product product;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setSku("BRK-PAD-01");
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Chennai Depot");
        warehouse.setCity("Chennai");
    }

    private void givenProductAndWarehouseExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
    }

    @Test
    void stockOutMoreThanAvailableIsRejectedAndNothingIsSaved() {
        givenProductAndWarehouseExist();
        StockLevel level = new StockLevel(product, warehouse);
        level.setQuantity(3);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(level));

        var request = new AdjustStockRequest(1L, 1L, AdjustmentType.OUT, 5, "too many");

        assertThatThrownBy(() -> stockService.adjust(request, "staff@test.dev"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 available");

        assertThat(level.getQuantity()).isEqualTo(3);           // unchanged
        verify(stockLevelRepository, never()).save(any());       // no update
        verify(movementRepository, never()).save(any());          // no ledger row
    }

    @Test
    void stockOutReducesQuantityAndWritesMovement() {
        givenProductAndWarehouseExist();
        StockLevel level = new StockLevel(product, warehouse);
        level.setQuantity(10);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(level));

        var response = stockService.adjust(
                new AdjustStockRequest(1L, 1L, AdjustmentType.OUT, 4, "repair"), "staff@test.dev");

        assertThat(response.quantity()).isEqualTo(6);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(MovementType.OUT);
        assertThat(movement.getQuantity()).isEqualTo(4);
        assertThat(movement.getCreatedBy()).isEqualTo("staff@test.dev");
    }

    @Test
    void stockInCreatesALevelWhenProductIsNewToTheWarehouse() {
        givenProductAndWarehouseExist();
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.empty());

        var response = stockService.adjust(
                new AdjustStockRequest(1L, 1L, AdjustmentType.IN, 10, "delivery"), "staff@test.dev");

        assertThat(response.quantity()).isEqualTo(10);
        verify(stockLevelRepository).save(any(StockLevel.class));
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void unknownProductGives404() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.adjust(
                new AdjustStockRequest(99L, 1L, AdjustmentType.IN, 1, null), "staff@test.dev"))
                .isInstanceOf(NotFoundException.class);
    }
}
