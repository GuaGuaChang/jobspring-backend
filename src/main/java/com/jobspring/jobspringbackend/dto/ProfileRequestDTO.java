package com.jobspring.jobspringbackend.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileRequestDTO {
    private ProfileDTO profile;
    private List<EducationDTO> education;
    private List<ExperienceDTO> experience;
    private List<UserSkillDTO> skills;
}