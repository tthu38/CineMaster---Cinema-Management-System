package com.example.cinemaster.validation;

import com.example.cinemaster.dto.request.WorkHistoryUpdateRequest;
import com.example.cinemaster.entity.WorkHistory;
import com.example.cinemaster.repository.WorkHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class NoShiftOverlapOnUpdateValidator implements ConstraintValidator<NoShiftOverlapOnUpdate, WorkHistoryUpdateRequest> {

    private final WorkHistoryRepository repo;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public boolean isValid(WorkHistoryUpdateRequest req, ConstraintValidatorContext ctx) {
        if (req == null || req.id() == null) return true;

        // Lấy bản ghi hiện tại để biết accountId/actionTime/action gốc nếu client không gửi
        WorkHistory current = repo.findById(req.id())
                .orElseThrow(() -> new EntityNotFoundException("WorkHistory not found: " + req.id()));

        Integer accountId = current.getAccountID() != null ? current.getAccountID().getAccountID() : null;
        String action     = req.action() != null ? req.action() : current.getAction();
        Instant actionTime= req.actionTime() != null ? req.actionTime() : current.getActionTime();

        if (accountId == null || action == null || actionTime == null) return true;

        ShiftType st;
        try {
            st = ShiftType.fromAction(action);
        } catch (Exception e) {
            return true;
        }
        Instant[] range = ShiftType.rangeForDay(actionTime, ZONE, st);

        boolean exists = repo.existsByAccountID_AccountIDAndActionAndIdNotAndActionTimeBetween(
                accountId, st.name(), req.id(), range[0], range[1]);

        if (exists) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                            "Nhân viên đã có ca " + st.name() + " (" +
                                    ShiftType.hhmm(st.start) + "–" + ShiftType.hhmm(st.end) +
                                    ") trong ngày này")
                    .addPropertyNode("action")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
