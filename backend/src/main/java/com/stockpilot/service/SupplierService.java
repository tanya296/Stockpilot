package com.stockpilot.service;

import com.stockpilot.dto.SupplierDtos.SupplierRequest;
import com.stockpilot.dto.SupplierDtos.SupplierResponse;
import com.stockpilot.entity.Supplier;
import com.stockpilot.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAll(Sort.by("name")).stream().map(SupplierResponse::from).toList();
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier s = new Supplier();
        s.setName(request.name().trim());
        s.setContact(request.contact());
        // A duplicate name hits the UNIQUE constraint -> DataIntegrityViolationException -> 409.
        return SupplierResponse.from(supplierRepository.saveAndFlush(s));
    }
}
