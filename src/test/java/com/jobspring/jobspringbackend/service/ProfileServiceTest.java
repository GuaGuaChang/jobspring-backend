package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileEducationRepository educationRepository;
    @Mock private ProfileExperienceRepository experienceRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ProfileService service;

    private User user;
    private Profile profile;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(100L);
        user.setFullName("Alice");

        profile = new Profile();
        profile.setId(10L);
        profile.setUser(user);
        profile.setSummary("Developer");
        profile.setVisibility(0);
        profile.setFileUrl("resume.pdf");
    }

    // ========= getCompleteProfile =========

    @Test
    void getCompleteProfile_shouldAggregateData() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        ProfileEducation edu = new ProfileEducation();
        edu.setSchool("NUS");
        edu.setDegree("BSc");
        edu.setMajor("CS");
        edu.setStartDate(LocalDate.of(2020, 1, 1));
        edu.setEndDate(LocalDate.of(2024, 1, 1));
        edu.setGpa(BigDecimal.valueOf(4.3));
        edu.setDescription("Excellent student");

        ProfileExperience exp = new ProfileExperience();
        exp.setCompany("OpenAI");
        exp.setTitle("Intern");
        exp.setStartDate(LocalDate.of(2023, 1, 1));
        exp.setEndDate(LocalDate.of(2023, 12, 1));
        exp.setDescription("Worked on ML");
        exp.setAchievements("Improved performance");

        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Java");
        UserSkill us = new UserSkill();
        us.setUser(user);
        us.setSkill(skill);
        us.setYears(BigDecimal.valueOf(2.0));
        us.setLevel(0);

        when(educationRepository.findByProfileId(10L)).thenReturn(List.of(edu));
        when(experienceRepository.findByProfileId(10L)).thenReturn(List.of(exp));
        when(userSkillRepository.findByUserId(100L)).thenReturn(List.of(us));

        ProfileResponseDTO result = service.getCompleteProfile(100L);

        assertNotNull(result);
        assertEquals("Developer", result.getProfile().getSummary());
        assertEquals(1, result.getEducation().size());
        assertEquals("NUS", result.getEducation().get(0).getSchool());
        assertEquals(1, result.getExperience().size());
        assertEquals("OpenAI", result.getExperience().get(0).getCompany());
        assertEquals(1, result.getSkills().size());
        assertEquals("Java", result.getSkills().get(0).getSkillName());
    }

    // ========= createOrUpdateProfile =========

    @Test
    void createOrUpdateProfile_shouldSaveProfileAndSubEntities() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // 准备 DTO
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setSummary("Updated summary");
        profileDTO.setVisibility(0);
        profileDTO.setFileUrl("new_resume.pdf");

        EducationDTO edu = new EducationDTO();
        edu.setSchool("NUS");
        edu.setDegree("BSc");
        edu.setMajor("CS");
        edu.setStartDate("2020-01-01");
        edu.setEndDate("2024-01-01");
        edu.setGpa(4.3);
        edu.setDescription("Graduated with honors");

        ExperienceDTO exp = new ExperienceDTO();
        exp.setCompany("OpenAI");
        exp.setTitle("Engineer");
        exp.setStartDate("2024-01-01");
        exp.setEndDate("2025-01-01");
        exp.setDescription("Worked on AI");
        exp.setAchievements("Innovation Award");

        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Java");

        UserSkillDTO usDTO = new UserSkillDTO();
        usDTO.setSkillId(1L);
        usDTO.setLevel(0);
        usDTO.setYears(2.0);

        when(skillRepository.findAllById(anyList())).thenReturn(List.of(skill));

        ProfileRequestDTO request = new ProfileRequestDTO();
        request.setProfile(profileDTO);
        request.setEducation(List.of(edu));
        request.setExperience(List.of(exp));
        request.setSkills(List.of(usDTO));

        ProfileUpdateResponseDTO response = service.createOrUpdateProfile(100L, request);

        assertEquals("success", response.getStatus());
        verify(profileRepository, atLeast(1)).save(any(Profile.class));
        verify(educationRepository, atLeastOnce()).save(any(ProfileEducation.class));
        verify(experienceRepository, atLeastOnce()).save(any(ProfileExperience.class));
        verify(userSkillRepository, atLeastOnce()).save(any(UserSkill.class));
    }

    // ========= createOrUpdateProfile 异常 =========

    @Test
    void createOrUpdateProfile_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(100L)).thenReturn(Optional.empty());

        ProfileRequestDTO req = new ProfileRequestDTO();
        req.setProfile(new ProfileDTO());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.createOrUpdateProfile(100L, req)
        );
        assertTrue(ex.getMessage().contains("User not found"));
    }

    // ========= handleSkills =========

    @Test
    void handleSkills_shouldThrow_whenSkillNotExist() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(skillRepository.findAllById(anyList())).thenReturn(List.of()); // 模拟技能找不到

        UserSkillDTO dto = new UserSkillDTO();
        dto.setSkillId(999L);
        dto.setLevel(1);
        dto.setYears(1.0);

        ProfileRequestDTO req = new ProfileRequestDTO();
        req.setProfile(new ProfileDTO());
        req.setSkills(List.of(dto));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.createOrUpdateProfile(100L, req)
        );

        // 验证异常链条内容
        assertTrue(ex.getMessage().contains("400 BAD_REQUEST"));
        assertTrue(ex.getMessage().contains("Unknown skill_id"));
    }

}
