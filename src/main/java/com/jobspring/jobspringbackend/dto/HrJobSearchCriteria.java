package com.jobspring.jobspringbackend.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HrJobSearchCriteria(
        String title,                 // 标题模糊
        Integer status,               // 0=有效 1=无效
        String location,              // 地点模糊
        Integer employmentType,       // 1/2/3...
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime postedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime postedTo,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String keyword
) {}