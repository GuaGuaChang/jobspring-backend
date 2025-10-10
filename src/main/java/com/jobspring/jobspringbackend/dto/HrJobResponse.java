package com.jobspring.jobspringbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HrJobResponse(
        Long id,
        String title,
        String location,
        Integer employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Integer status,
        LocalDateTime postedAt
) {}