package com.phegondev.InventoryManagementSystem.service.impl;


import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.SupplierDTO;
import com.phegondev.InventoryManagementSystem.entity.Supplier;
import com.phegondev.InventoryManagementSystem.entity.SupplierProfile;
import com.phegondev.InventoryManagementSystem.exceptions.NotFoundException;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierProfileRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ModelMapper modelMapper;
    private final SupplierProfileRepository supplierProfileRepository;
    private final TransactionRepository transactionRepository;

    private SupplierDTO toDto(Supplier supplier) {
        SupplierDTO dto = modelMapper.map(supplier, SupplierDTO.class);
        supplierProfileRepository.findBySupplier_Id(supplier.getId()).ifPresent(p -> {
            dto.setEmail(p.getEmail());
            dto.setCategorySpecialisation(p.getCategorySpecialisation());
            dto.setPaymentTerms(p.getPaymentTerms());
            dto.setStarRating(p.getStarRating());
            dto.setOnTimeDeliveryPercent(p.getOnTimeDeliveryPercent());
            dto.setActive(p.getActive());
        });

        BigDecimal total = transactionRepository.sumPurchasesBySupplier(supplier.getId());
        Long units = transactionRepository.sumPurchaseUnitsBySupplier(supplier.getId());
        dto.setTotalPurchaseValue(total == null ? BigDecimal.ZERO : total);
        dto.setItemsSupplied(units == null ? 0L : units);
        return dto;
    }

    @Override
    public Response addSupplier(SupplierDTO supplierDTO) {
        Supplier supplierToSave = modelMapper.map(supplierDTO, Supplier.class);
        supplierRepository.save(supplierToSave);

        SupplierProfile profile = SupplierProfile.builder()
                .supplier(supplierToSave)
                .email(supplierDTO.getEmail())
                .categorySpecialisation(supplierDTO.getCategorySpecialisation())
                .paymentTerms(supplierDTO.getPaymentTerms())
                .starRating(supplierDTO.getStarRating())
                .onTimeDeliveryPercent(supplierDTO.getOnTimeDeliveryPercent())
                .active(supplierDTO.getActive() == null ? Boolean.TRUE : supplierDTO.getActive())
                .qualityScorePercent(null)
                .responseTimeHours(null)
                .build();
        supplierProfileRepository.save(profile);

        return Response.builder()
                .status(200)
                .message("Supplier added successfully")
                .supplierId(supplierToSave.getId())
                .build();
    }

    @Override
    public Response updateSupplier(Long id, SupplierDTO supplierDTO) {

        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        if (supplierDTO.getName() != null) existingSupplier.setName(supplierDTO.getName());
        if (supplierDTO.getAddress() != null) existingSupplier.setAddress(supplierDTO.getAddress());

        supplierRepository.save(existingSupplier);

        SupplierProfile profile = supplierProfileRepository.findBySupplier_Id(id)
                .orElseGet(() -> SupplierProfile.builder().supplier(existingSupplier).build());
        if (supplierDTO.getEmail() != null) profile.setEmail(supplierDTO.getEmail());
        if (supplierDTO.getCategorySpecialisation() != null) profile.setCategorySpecialisation(supplierDTO.getCategorySpecialisation());
        if (supplierDTO.getPaymentTerms() != null) profile.setPaymentTerms(supplierDTO.getPaymentTerms());
        if (supplierDTO.getStarRating() != null) profile.setStarRating(supplierDTO.getStarRating());
        if (supplierDTO.getOnTimeDeliveryPercent() != null) profile.setOnTimeDeliveryPercent(supplierDTO.getOnTimeDeliveryPercent());
        if (supplierDTO.getActive() != null) profile.setActive(supplierDTO.getActive());
        supplierProfileRepository.save(profile);

        return Response.builder()
                .status(200)
                .message("Supplier Successfully Updated")
                .supplierId(existingSupplier.getId())
                .build();
    }

    @Override
    public Response getAllSuppliers() {

        List<Supplier> categories = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<SupplierDTO> supplierDTOS = categories.stream().map(this::toDto).toList();

        return Response.builder()
                .status(200)
                .message("success")
                .suppliers(supplierDTOS)
                .build();
    }

    @Override
    public Response getSupplierById(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        SupplierDTO supplierDTO = toDto(supplier);

        return Response.builder()
                .status(200)
                .message("success")
                .supplier(supplierDTO)
                .build();
    }

    @Override
    public Response deleteSupplier(Long id) {

        supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        supplierRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Supplier Successfully Deleted")
                .build();
    }
}
