package com.jobspring.jobspringbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 1xxx - generic client errors
    INVALID_ARGUMENT(1001, HttpStatus.BAD_REQUEST, "Invalid argument"),
    VALIDATION_FAILED(1002, HttpStatus.BAD_REQUEST, "Validation failed"),
    JSON_PARSE_ERROR(1003, HttpStatus.BAD_REQUEST, "Malformed JSON"),

    // 11xx - auth
    UNAUTHORIZED(1101, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    TOKEN_EXPIRED(1102, HttpStatus.UNAUTHORIZED, "Token expired"),
    FORBIDDEN(1103, HttpStatus.FORBIDDEN, "Forbidden"),

    // 14xx - resource/state
    NOT_FOUND(1404, HttpStatus.NOT_FOUND, "Not found"),
    CONFLICT(1409, HttpStatus.CONFLICT, "Conflict"),
    TOO_MANY_REQUESTS(1429, HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),

    // 2xxx - domain specific
    VERIFY_TOO_FREQUENT(2001, HttpStatus.TOO_MANY_REQUESTS, "Verification too frequent"),
    VERIFY_CODE_INVALID(2002, HttpStatus.BAD_REQUEST, "Invalid verification code"),

    // 5xxx - server side
    INTERNAL_ERROR(1500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
