package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Long applicationId;
    private String title;
    private String content;
    private Integer rating;
    private Integer status;
    private LocalDateTime submittedAt;
    private Long reviewedById;
    private String reviewNote;
    private LocalDateTime publicAt;
}
