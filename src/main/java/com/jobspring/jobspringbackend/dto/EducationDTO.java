package com.jobspring.jobspringbackend.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EducationDTO {
    private String school;
    private String degree;
    private String major;
    private String startDate;
    private String endDate;
    private Double gpa;
    private String description;
}
