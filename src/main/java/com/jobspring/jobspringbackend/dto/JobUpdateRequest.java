package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobUpdateRequest {
    private String title;
    private String location;
    private Integer employmentType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String description;
    private Integer status; // 0=上架 1=下线
}
