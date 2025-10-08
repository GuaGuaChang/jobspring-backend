package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyReviewDTO {
    private Long reviewId;
    private Long companyId;
    private String title;
    private String content;
    private Integer rating;
    private LocalDateTime publicAt;
    private String imageUrl;
}
