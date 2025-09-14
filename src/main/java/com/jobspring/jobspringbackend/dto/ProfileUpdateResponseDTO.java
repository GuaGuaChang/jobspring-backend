package com.jobspring.jobspringbackend.dto;


import lombok.Data;

@Data
public class ProfileUpdateResponseDTO {
    private String status;
    private String message;
    private Long profileId;

    public ProfileUpdateResponseDTO(String status, String message, Long profileId) {
        this.status = status;
        this.message = message;
        this.profileId = profileId;
    }
}
