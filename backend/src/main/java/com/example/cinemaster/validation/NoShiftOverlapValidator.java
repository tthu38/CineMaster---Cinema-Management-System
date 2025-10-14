package com.example.cinemaster.validation;

import com.example.cinemaster.dto.request.WorkScheduleCreateRequest;
import com.example.cinemaster.repository.WorkScheduleRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoShiftOverlapValidator implements ConstraintValidator<NoShiftOverlap, WorkScheduleCreateRequest> {

    private final WorkScheduleRepository repo;

    @Override
    public boolean isValid(WorkScheduleCreateRequest req, ConstraintValidatorContext ctx) {
        if (req == null) return true;

        // ===== Kiểm tra thời gian hợp lệ =====
        if (req.getStartTime() != null && req.getEndTime() != null && !req.getStartTime().isBefore(req.getEndTime())) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("StartTime phải nhỏ hơn EndTime")
                    .addPropertyNode("startTime").addConstraintViolation();
            return false;
        }

        if (req.getAccountId() == null || req.getShiftDate() == null ||
                req.getStartTime() == null || req.getEndTime() == null) {
            return true; // để @NotNull khác xử lý
        }

        // ===== Kiểm tra trùng ca =====
        boolean overlapped = repo.existsByAccount_AccountIDAndShiftDateAndStartTimeLessThanAndEndTimeGreaterThan(
                req.getAccountId(),
                req.getShiftDate(),
                req.getEndTime(),
                req.getStartTime()
        );

        if (overlapped) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Trùng ca với lịch khác trong ngày")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
