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

    // ===== CREATE =====
    public DiscountResponse create(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }
        Discount discount = discountMapper.toEntity(request);
        discount.setCreateAt(LocalDate.now());
        discount.setDiscountStatus("ACTIVE");
        discountRepository.save(discount);
        return discountMapper.toResponse(discount);
    }

    // ===== AUTO STATUS UPDATE =====
    private void autoUpdateStatus(Discount discount) {
        if (discount.getExpiryDate() != null
                && discount.getExpiryDate().isBefore(LocalDate.now())
                && !"DELETED".equalsIgnoreCase(discount.getDiscountStatus())) {
            discount.setDiscountStatus("EXPIRED");
            discountRepository.save(discount);
        }
    }

    // ===== READ ALL =====
    public List<DiscountResponse> getAll() {
        return discountRepository.findAll().stream()
                .peek(this::autoUpdateStatus)
                .filter(d -> !"DELETED".equalsIgnoreCase(d.getDiscountStatus()))
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== READ BY STATUS =====
    public List<DiscountResponse> getByStatus(String status) {
        return discountRepository.findAll().stream()
                .peek(this::autoUpdateStatus)
                .filter(d -> d.getDiscountStatus().equalsIgnoreCase(status))
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== READ BY ID =====
    public DiscountResponse getById(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));
        autoUpdateStatus(discount);
        return discountMapper.toResponse(discount);
    }

    // ===== UPDATE =====
    public DiscountResponse update(Integer id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        if (!existing.getCode().equals(request.getCode())
                && discountRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.DISCOUNT_CODE_EXISTS);
        }

        discountMapper.updateDiscountFromRequest(request, existing);
        autoUpdateStatus(existing);
        discountRepository.save(existing);

        return discountMapper.toResponse(existing);
    }

    // ===== SOFT DELETE =====
    public void softDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));
        discount.setDiscountStatus("DELETED");
        discountRepository.save(discount);
    }

    // ===== RESTORE =====
    public void restore(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));

        if (!"DELETED".equalsIgnoreCase(discount.getDiscountStatus())) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT);
        }

        // Nếu hết hạn thì chuyển sang EXPIRED, ngược lại ACTIVE
        if (discount.getExpiryDate() != null && discount.getExpiryDate().isBefore(LocalDate.now())) {
            discount.setDiscountStatus("EXPIRED");
        } else {
            discount.setDiscountStatus("ACTIVE");
        }
        discountRepository.save(discount);
    }

    // ===== HARD DELETE (optional) =====
    public void hardDelete(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISCOUNT_NOT_FOUND));
        discountRepository.delete(discount);
    }
}
