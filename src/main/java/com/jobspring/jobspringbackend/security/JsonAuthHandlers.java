package com.jobspring.jobspringbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobspring.jobspringbackend.exception.ApiError;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JsonAuthHandlers {
    private final ObjectMapper mapper;

    public void write(HttpServletRequest req, HttpServletResponse resp,
                      ErrorCode ec, String message) throws IOException {
        resp.setStatus(ec.getHttpStatus().value());
        resp.setContentType("application/json;charset=UTF-8");
        var body = ApiError.builder()
                .code(ec.getCode())
                .message(message != null ? message : ec.getDefaultMessage())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .build();
        mapper.writeValue(resp.getWriter(), body);
    }
}
