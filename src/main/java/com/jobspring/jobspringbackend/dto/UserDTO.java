package com.jobspring.jobspringbackend.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private Integer role;
    private Boolean isActive;
}
