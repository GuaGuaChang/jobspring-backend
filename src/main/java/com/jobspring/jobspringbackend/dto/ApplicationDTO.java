package com.jobspring.jobspringbackend.dto;

import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

@Data
public class ApplicationDTO {
    // TODO: DTO fields
    @Column(length = 5000)
    private String resumeProfile;

}
