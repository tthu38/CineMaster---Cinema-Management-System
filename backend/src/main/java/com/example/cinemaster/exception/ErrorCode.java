package com.example.cinemaster.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    INVALID_INPUT(1001, "Invalid input data", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1002, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1003, "User not found", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1004, "Unauthorized access", HttpStatus.FORBIDDEN),
    EMAIL_NOT_FOUND_EXCEPTION(1005, "Email not found", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(1006, "Invalid password", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1007, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1008, "Token is expired", HttpStatus.UNAUTHORIZED),
    PHONE_NOT_FOUND(1009, "Phone not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(1010, "Role not found", HttpStatus.NOT_FOUND),
    INVALID_EMAIL_FORMAT(1011, "Invalid email format", HttpStatus.BAD_REQUEST),
    DISCOUNT_NOT_FOUND(1012, "Discount not found", HttpStatus.NOT_FOUND),
    DISCOUNT_CODE_EXISTS(1013, "Discount code already exists", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT(1014, "Invalid discount data", HttpStatus.BAD_REQUEST),
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
