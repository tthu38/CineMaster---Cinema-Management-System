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

        if (req.startTime() != null && req.endTime() != null && !req.startTime().isBefore(req.endTime())) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("StartTime phải nhỏ hơn EndTime")
                    .addPropertyNode("startTime").addConstraintViolation();
            return false;
        }
        if (req.accountId() == null || req.shiftDate() == null || req.startTime() == null || req.endTime() == null) {
            return true; // để @NotNull khác xử lý
        }

        boolean overlapped = repo.existsByAccountID_AccountIDAndShiftDateAndStartTimeLessThanAndEndTimeGreaterThan(
                req.accountId(), req.shiftDate(), req.endTime(), req.startTime());
        if (overlapped) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Trùng ca với lịch khác trong ngày")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
