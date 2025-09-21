package com.jobspring.jobspringbackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfileResponseDTO {
    private ProfileDTO profile;
    private List<EducationDTO> education;
    private List<ExperienceDTO> experience;
    private List<UserSkillDTO> skills;
}

