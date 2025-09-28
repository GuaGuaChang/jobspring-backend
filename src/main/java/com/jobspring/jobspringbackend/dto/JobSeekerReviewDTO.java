package com.jobspring.jobspringbackend.dto;

import lombok.Data;

@Data
public class JobSeekerReviewDTO {
    private Long applicationId;
    private String title;
    private String content;
    private Integer rating;
}
