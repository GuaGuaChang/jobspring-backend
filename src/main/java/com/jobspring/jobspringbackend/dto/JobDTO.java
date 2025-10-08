package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDTO {
    private Long id;
    private String title;
    private Long companyId;
    private String company;
    private String location;
    private String employmentType;
    private List<String> tags;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private LocalDateTime postedAt;
    private String description;
}
