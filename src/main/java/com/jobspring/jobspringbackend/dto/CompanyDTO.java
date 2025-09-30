package com.jobspring.jobspringbackend.dto;


import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;
    private String name;
    private String website;
    private Integer size;
    private String logoUrl;
    private String description;
    private String createdBy;
}

