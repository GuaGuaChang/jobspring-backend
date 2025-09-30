package com.jobspring.jobspringbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobspring.jobspringbackend.exception.ApiError;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

public class JsonAuthHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void write(HttpServletRequest req, HttpServletResponse resp, ErrorCode ec, String message) throws IOException {
        resp.setStatus(ec.getHttpStatus().value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = ApiError.builder()
                .code(ec.getCode())
                .message(message != null ? message : ec.getDefaultMessage())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .build();
        MAPPER.writeValue(resp.getWriter(), body);
    }
}
