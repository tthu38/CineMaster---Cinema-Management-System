package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.entity.Discount;
import com.example.cinemaster.exception.AppException;
import com.example.cinemaster.exception.ErrorCode;
import com.example.cinemaster.mapper.DiscountMapper;
import com.example.cinemaster.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;

    // Create
    public DiscountResponse create(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }

        Discount discount = discountMapper.toEntity(request);
        // Mapper đã set createAt + status ACTIVE trong @AfterMapping
        discountRepository.save(discount);

        return discountMapper.toResponse(discount);
    }

    public List<DiscountResponse> getAll() {
        List<Discount> discounts = discountRepository.findAll();

        discounts.forEach(this::autoUpdateStatus);

        return discounts.stream()
                .filter(d -> !"DELETED".equalsIgnoreCase(d.getDiscountStatus()))
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<DiscountResponse> getByStatus(String status) {
        List<Discount> discounts = discountRepository.findAll();

        discounts.forEach(this::autoUpdateStatus);

        return discounts.stream()
                .filter(d -> d.getDiscountStatus().equalsIgnoreCase(status))
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DiscountResponse getById(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        autoUpdateStatus(discount);

        return discountMapper.toResponse(discount);
    }


    public DiscountResponse update(Integer id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        if (!existing.getCode().equalsIgnoreCase(request.getCode()) &&
                discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }

        discountMapper.updateDiscountFromRequest(request, existing);
        autoUpdateStatus(existing);
        discountRepository.save(existing);

        return discountMapper.toResponse(existing);
    }

    public void softDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));
        discount.setDiscountStatus("Inactive"); // ✅ soft delete -> inactive
        discountRepository.save(discount);
    }

    public void restore(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        if (!"Inactive".equalsIgnoreCase(discount.getDiscountStatus())) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT);
        }

        if (discount.getExpiryDate() != null &&
                discount.getExpiryDate().isBefore(LocalDate.now())) {
            discount.setDiscountStatus("Expired");
        } else {
            discount.setDiscountStatus("Active");
        }

        discountRepository.save(discount);
    }
    public void hardDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        discountRepository.delete(discount);
    }

    private void autoUpdateStatus(Discount discount) {
        if (discount.getExpiryDate() == null) return;

        boolean expired = discount.getExpiryDate().isBefore(LocalDate.now());
        String status = discount.getDiscountStatus();

        if (expired && !"EXPIRED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(status)) {
            discount.setDiscountStatus("EXPIRED");
            discountRepository.save(discount);
        }
    }
}
