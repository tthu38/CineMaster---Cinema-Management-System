package com.example.cinemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoShiftOverlapValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoShiftOverlap {
    String message() default "Trùng ca làm việc với nhân viên này trong ngày đã chọn";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
