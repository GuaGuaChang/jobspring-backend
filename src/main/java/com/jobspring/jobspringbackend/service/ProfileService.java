package com.jobspring.jobspringbackend.service;


import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileEducationRepository educationRepository;

    @Autowired
    private ProfileExperienceRepository experienceRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    public ProfileResponseDTO getCompleteProfile(Long userId) {
        // 获取基础profile信息
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // 获取教育经历
        List<ProfileEducation> educations = educationRepository.findByProfileId(profile.getId());

        // 获取工作经历
        List<ProfileExperience> experiences = experienceRepository.findByProfileId(profile.getId());

        // 获取技能
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);

        // 转换为DTO
        return convertToResponseDTO(profile, educations, experiences, skills);
    }

    @Transactional
    public ProfileUpdateResponseDTO createOrUpdateProfile(Long userId, ProfileRequestDTO request) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            Profile profile = handleProfile(user, request.getProfile());
            Profile savedProfile = profileRepository.save(profile);

            profileRepository.flush();

            handleEducations(savedProfile, request.getEducation());
            handleExperiences(savedProfile, request.getExperience());
            handleSkills(userId, request.getSkills());

            return new ProfileUpdateResponseDTO("success", "Profile updated successfully", savedProfile.getId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to update profile: " + e.getMessage());
        }
    }

    private ProfileResponseDTO convertToResponseDTO(Profile profile,
                                                    List<ProfileEducation> educations,
                                                    List<ProfileExperience> experiences,
                                                    List<UserSkill> skills) {
        ProfileResponseDTO response = new ProfileResponseDTO();

        // 设置profile信息
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setSummary(profile.getSummary());
        profileDTO.setVisibility(profile.getVisibility());
        profileDTO.setFileUrl(profile.getFileUrl());
        response.setProfile(profileDTO);

        // 设置教育经历
        response.setEducation(educations.stream().map(this::convertToEducationDTO).collect(Collectors.toList()));

        // 设置工作经历
        response.setExperience(experiences.stream().map(this::convertToExperienceDTO).collect(Collectors.toList()));

        // 设置技能
        response.setSkills(skills.stream().map(this::convertToUserSkillDTO).collect(Collectors.toList()));

        return response;
    }

    private EducationDTO convertToEducationDTO(ProfileEducation education) {
        EducationDTO dto = new EducationDTO();
        dto.setSchool(education.getSchool());
        dto.setDegree(education.getDegree());
        dto.setMajor(education.getMajor());
        dto.setStartDate(education.getStartDate() == null ? null : education.getStartDate().toString());
        dto.setEndDate(education.getEndDate() == null ? null : education.getEndDate().toString());
        dto.setGpa(education.getGpa() != null ? education.getGpa().doubleValue() : null);
        dto.setDescription(education.getDescription());
        return dto;
    }

    private ExperienceDTO convertToExperienceDTO(ProfileExperience experience) {
        ExperienceDTO dto = new ExperienceDTO();
        dto.setCompany(experience.getCompany());
        dto.setTitle(experience.getTitle());
        dto.setStartDate(experience.getStartDate() == null ? null : experience.getStartDate().toString());
        dto.setEndDate(experience.getEndDate() == null ? null : experience.getEndDate().toString());
        dto.setDescription(experience.getDescription());
        dto.setAchievements(experience.getAchievements());
        return dto;
    }

    private UserSkillDTO convertToUserSkillDTO(UserSkill userSkill) {
        UserSkillDTO dto = new UserSkillDTO();
        dto.setSkillId(userSkill.getSkill() != null ? userSkill.getSkill().getId() : null);
        dto.setSkillName(userSkill.getSkill() != null ? userSkill.getSkill().getName() : null);
        dto.setLevel(userSkill.getLevel());
        dto.setYears(userSkill.getYears().doubleValue());
        return dto;
    }

    private Profile handleProfile(User user, ProfileDTO profileDTO) {
        // 直接使用User对象查询
        Profile profile = profileRepository.findByUser(user)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);  // 设置关联用户
                    return newProfile;
                });

        // 更新profile信息
        profile.setSummary(profileDTO.getSummary());
        profile.setVisibility(profileDTO.getVisibility());
        profile.setFileUrl(profileDTO.getFileUrl());

        return profile;
    }

    private void handleEducations(Profile profile, List<EducationDTO> educationDTOs) {
        if (educationDTOs == null) return;

        Profile savedProfile = profileRepository.save(profile);

        educationRepository.deleteByProfileId(savedProfile.getId());

        for (EducationDTO dto : educationDTOs) {
            ProfileEducation education = new ProfileEducation();
            education.setProfile(savedProfile);
            education.setSchool(dto.getSchool());
            education.setDegree(dto.getDegree());
            education.setMajor(dto.getMajor());
            education.setStartDate(Date.valueOf(dto.getStartDate()).toLocalDate());
            education.setEndDate(Date.valueOf(dto.getEndDate()).toLocalDate());
            education.setGpa(dto.getGpa() != null ? BigDecimal.valueOf(dto.getGpa()) : null);
            education.setDescription(dto.getDescription());

            educationRepository.save(education);
        }
    }

    private void handleExperiences(Profile profile, List<ExperienceDTO> experienceDTOs) {
        if (experienceDTOs == null) return;

        Profile savedProfile = profileRepository.save(profile);

        experienceRepository.deleteByProfileId(savedProfile.getId());

        for (ExperienceDTO dto : experienceDTOs) {
            ProfileExperience experience = new ProfileExperience();
            experience.setProfile(savedProfile);
            experience.setCompany(dto.getCompany());
            experience.setTitle(dto.getTitle());
            experience.setStartDate(Date.valueOf(dto.getStartDate()).toLocalDate());
            experience.setEndDate(dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()).toLocalDate() : null);
            experience.setDescription(dto.getDescription());
            experience.setAchievements(dto.getAchievements());

            experienceRepository.save(experience);
        }
    }

    private void handleSkills(Long userId, List<UserSkillDTO> skillDTOs) {
        if (skillDTOs == null) return;             // 传 null 表示不改动，保留旧数据
        if (skillDTOs.isEmpty()) {
            return;
        }

        Map<Long, UserSkillDTO> byId = new LinkedHashMap<>();
        for (UserSkillDTO dto : skillDTOs) {
            if (dto.getSkillId() != null) {
                byId.put(dto.getSkillId(), dto); // 后来的覆盖前面的，或可自定义策略
            }
        }
        List<UserSkillDTO> deduped = new ArrayList<>(byId.values());

        List<Long> ids = deduped.stream()
                .map(UserSkillDTO::getSkillId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Skill> skillMap = skillRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Skill::getId, s -> s));
        List<Long> missing = ids.stream().filter(id -> !skillMap.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown skill_id(s): " + missing);
        }

        userSkillRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        for (UserSkillDTO dto : deduped) {
            Skill skill = skillMap.get(dto.getSkillId());
            UserSkill us = new UserSkill();
            us.setUser(user);
            us.setSkill(skill);
            us.setLevel(dto.getLevel());
            us.setYears(BigDecimal.valueOf(dto.getYears()));
            userSkillRepository.save(us);
        }
    }

}