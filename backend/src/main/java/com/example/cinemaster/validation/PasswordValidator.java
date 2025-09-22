package com.example.cinemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final String PASSWORD_PATTERN =
            "^(?=.{8,})(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-={}:;<>?,.])[A-Z][A-Za-z0-9!@#$%^&*()_+\\-={}:;<>?,.]*$";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        return password.matches(PASSWORD_PATTERN);
    }
}
