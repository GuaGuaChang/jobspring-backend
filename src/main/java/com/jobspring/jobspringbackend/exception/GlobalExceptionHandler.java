package com.jobspring.jobspringbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Unified business exception ----
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiError> handleBiz(BizException ex, HttpServletRequest req) {
        return build(ex.getErrorCode(), ex.getMessage(), req, null);
    }

    // ---- Bean Validation on @RequestBody DTO ----
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiError> handleValidation(Exception ex, HttpServletRequest req) {
        List<Map<String, Object>> errors;
        if (ex instanceof MethodArgumentNotValidException manv) {
            errors = manv.getBindingResult().getFieldErrors().stream()
                    .map(fe -> Map.<String, Object>of(
                            "field", fe.getField(),
                            "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Validation error",
                            "rejected", fe.getRejectedValue() != null ? fe.getRejectedValue() : ""
                    ))
                    .collect(Collectors.toList());
        } else {
            BindException be = (BindException) ex;
            errors = be.getBindingResult().getFieldErrors().stream()
                    .map(fe -> Map.<String, Object>of(
                            "field", fe.getField(),
                            "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Validation error",
                            "rejected", fe.getRejectedValue() != null ? fe.getRejectedValue() : ""
                    ))
                    .collect(Collectors.toList());
        }
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(), req, errors);
    }

    // ---- Constraint violations on @RequestParam / @PathVariable ----
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
                .map(v -> Map.<String, Object>of(
                        "param", v.getPropertyPath().toString(),
                        "message", v.getMessage() != null ? v.getMessage() : "Validation error",
                        "invalid", v.getInvalidValue() != null ? v.getInvalidValue() : ""
                ))
                .collect(Collectors.toList());
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(), req, errors);
    }

    // ---- Other common errors ----
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(ErrorCode.JSON_PARSE_ERROR, "Malformed JSON", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        List<Map<String, Object>> errors = List.of(
                Map.<String, Object>of("param", ex.getParameterName(), "message", "Missing required parameter")
        );
        return build(ErrorCode.INVALID_ARGUMENT, "Missing required parameter", req, errors);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(ErrorCode.INVALID_ARGUMENT, "Method not allowed", req, null);
    }

    // ---- Fallback 500 ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        return build(ErrorCode.INTERNAL_ERROR, "Internal error", req, null);
    }

    // ---- Helper ----
    private ResponseEntity<ApiError> build(ErrorCode ec, String message, HttpServletRequest req, List<Map<String, Object>> errors) {
        ApiError body = ApiError.builder()
                .code(ec.getCode())
                .message(message != null ? message : ec.getDefaultMessage())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .errors(errors)
                .build();
        return ResponseEntity.status(ec.getHttpStatus()).body(body);
    }
}
