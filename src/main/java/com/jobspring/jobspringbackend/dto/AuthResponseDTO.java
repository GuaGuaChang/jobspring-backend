package com.jobspring.jobspringbackend.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private UserDto user;

    @Data
    public static class UserDto {
        private Long id;
        private String email;
        private String fullName;

        private String roleName; // e.g. "ROLE_CANDIDATE"
        private Integer role;
    }
}