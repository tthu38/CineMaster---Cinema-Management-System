package com.example.cinemaster.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    INVALID_INPUT(1001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1003, "Không tìm thấy người dùng", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1004, "Truy cập bị từ chối", HttpStatus.FORBIDDEN),
    EMAIL_NOT_FOUND_EXCEPTION(1005, "Không tìm thấy email", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(1006, "Mật khẩu không đúng", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1007, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1008, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    PHONE_NOT_FOUND(1009, "Không tìm thấy số điện thoại", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(1010, "Không tìm thấy vai trò", HttpStatus.NOT_FOUND),
    INVALID_EMAIL_FORMAT(1011, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),

    DISCOUNT_NOT_FOUND(1012, "Không tìm thấy mã giảm giá", HttpStatus.NOT_FOUND),
    DISCOUNT_CODE_EXISTS(1013, "Mã giảm giá đã tồn tại", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT(1014, "Dữ liệu giảm giá không hợp lệ", HttpStatus.BAD_REQUEST),
    DISCOUNT_EXPIRED(1018, "Mã giảm giá đã hết hạn", HttpStatus.BAD_REQUEST),
    DISCOUNT_LIMIT_REACHED(1019, "Đã đạt giới hạn sử dụng mã giảm giá", HttpStatus.BAD_REQUEST),
    DISCOUNT_ALREADY_APPLIED(1020, "Mã giảm giá này đã được áp dụng cho vé", HttpStatus.BAD_REQUEST),
    DISCOUNT_MIN_ORDER_NOT_MET(1021, "Chưa đạt giá trị đơn hàng tối thiểu để áp dụng mã giảm giá", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT_VALUE(1022, "Giá trị giảm giá không hợp lệ", HttpStatus.BAD_REQUEST),

    MEMBERSHIP_REQUIRED(1023, "Yêu cầu có hạng thành viên để sử dụng mã giảm giá này", HttpStatus.FORBIDDEN),
    MEMBERSHIP_LEVEL_TOO_LOW(1024, "Cấp độ thành viên của bạn không đủ để sử dụng mã giảm giá này", HttpStatus.FORBIDDEN),

    TICKET_NOT_FOUND(1025, "Không tìm thấy vé", HttpStatus.NOT_FOUND),
    INVALID_TICKET_STATUS(1026, "Trạng thái vé không hợp lệ (phải ở trạng thái HOLDING)", HttpStatus.BAD_REQUEST),

    AUDITORIUM_NOT_FOUND(1015, "Không tìm thấy phòng chiếu", HttpStatus.NOT_FOUND),
    AUDITORIUM_INACTIVE(1016, "Phòng chiếu hiện đang bị vô hiệu hóa", HttpStatus.BAD_REQUEST),
    AUDITORIUM_ALREADY_EXISTS(1017, "Phòng chiếu đã tồn tại trong chi nhánh này", HttpStatus.BAD_REQUEST),
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
