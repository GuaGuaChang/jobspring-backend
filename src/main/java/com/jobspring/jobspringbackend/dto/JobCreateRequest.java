package com.jobspring.jobspringbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobCreateRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String location;

    @NotNull
    private Integer employmentType; // 1=全职 2=实习 3=合同工

    @PositiveOrZero
    private BigDecimal salaryMin;

    @PositiveOrZero
    private BigDecimal salaryMax;

    @Size(max = 20000)
    private String description;
}
