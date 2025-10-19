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

    // --- 🔹 DISCOUNT ---
    DISCOUNT_NOT_FOUND(1012, "Discount not found", HttpStatus.NOT_FOUND),
    DISCOUNT_CODE_EXISTS(1013, "Discount code already exists", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT(1014, "Invalid discount data", HttpStatus.BAD_REQUEST),
    DISCOUNT_EXPIRED(1018, "Discount code has expired", HttpStatus.BAD_REQUEST),
    DISCOUNT_LIMIT_REACHED(1019, "Discount usage limit reached", HttpStatus.BAD_REQUEST),
    DISCOUNT_ALREADY_APPLIED(1020, "This discount code is already applied to the ticket", HttpStatus.BAD_REQUEST),
    DISCOUNT_MIN_ORDER_NOT_MET(1021, "Minimum order amount not met for this discount", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT_VALUE(1022, "Invalid discount value", HttpStatus.BAD_REQUEST),

    // --- 🔹 MEMBERSHIP ---
    MEMBERSHIP_REQUIRED(1023, "Membership required to use this discount", HttpStatus.FORBIDDEN),
    MEMBERSHIP_LEVEL_TOO_LOW(1024, "Your membership level is too low for this discount", HttpStatus.FORBIDDEN),

    // --- 🔹 TICKET ---
    TICKET_NOT_FOUND(1025, "Ticket not found", HttpStatus.NOT_FOUND),
    INVALID_TICKET_STATUS(1026, "Ticket is not in HOLDING status", HttpStatus.BAD_REQUEST),

    // --- 🔹 AUDITORIUM ---
    AUDITORIUM_NOT_FOUND(1015, "Auditorium not found", HttpStatus.NOT_FOUND),
    AUDITORIUM_INACTIVE(1016, "Auditorium is inactive", HttpStatus.BAD_REQUEST),
    AUDITORIUM_ALREADY_EXISTS(1017, "Auditorium already exists in this branch", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
