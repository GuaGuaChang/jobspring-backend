package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteJobResponse {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private Integer employmentType;
    private Integer status;
    private LocalDateTime favoritedAt;
}
