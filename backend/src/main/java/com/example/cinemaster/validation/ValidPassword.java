package com.example.cinemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Mật khẩu không hợp lệ. Yêu cầu >= 8 ký tự, bắt đầu bằng chữ cái viết hoa, chứa ít nhất 1 số và 1 ký tự đặc biệt.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
