package com.jobspring.jobspringbackend.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private int code;                   // business code
    private String message;             // human-readable message
    private String path;                // request URI
    private Instant timestamp;          // server time
    private List<Map<String, Object>> errors; // field/param details
}
