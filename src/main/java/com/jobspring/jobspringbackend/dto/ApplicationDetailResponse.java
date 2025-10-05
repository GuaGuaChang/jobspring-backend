package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationDetailResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private Integer status;
    private LocalDateTime appliedAt;
    private String resumeUrl;
    private String resumeProfile;
}