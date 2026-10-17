package com.stockpilot.service;

import com.stockpilot.dto.WarehouseDtos.WarehouseRequest;
import com.stockpilot.dto.WarehouseDtos.WarehouseResponse;
import com.stockpilot.entity.Warehouse;
import com.stockpilot.exception.ConflictException;
import com.stockpilot.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> findAll() {
        return warehouseRepository.findAll(Sort.by("name")).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        if (warehouseRepository.existsByName(request.name().trim())) {
            throw new ConflictException("A warehouse with this name already exists.");
        }
        Warehouse w = new Warehouse();
        w.setName(request.name().trim());
        w.setCity(request.city().trim());
        return WarehouseResponse.from(warehouseRepository.save(w));
    }
}
