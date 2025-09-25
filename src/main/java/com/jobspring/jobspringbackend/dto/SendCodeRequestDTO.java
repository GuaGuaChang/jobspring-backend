package com.jobspring.jobspringbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeRequestDTO {
    @Email
    @NotBlank
    private String email;
}
