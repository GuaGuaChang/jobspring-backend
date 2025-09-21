package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobResponse {
    private Long id;
    private Long companyId;
    private String title;
    private String location;
    private Integer employmentType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String description;
    private Integer status;
    private LocalDateTime postedAt;
}
