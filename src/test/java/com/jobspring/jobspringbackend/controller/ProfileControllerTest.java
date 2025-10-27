package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ProfileDTO;
import com.jobspring.jobspringbackend.dto.ProfileRequestDTO;
import com.jobspring.jobspringbackend.dto.ProfileResponseDTO;
import com.jobspring.jobspringbackend.dto.ProfileUpdateResponseDTO;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "9", roles = "CANDIDATE")
    void testGetMyProfile() throws Exception {
        ProfileDTO profile = new ProfileDTO();
        profile.setSummary("Backend Developer");
        profile.setVisibility(1);
        profile.setFileUrl("https://cdn.example.com/profile1.pdf");

        ProfileResponseDTO mockResponse = new ProfileResponseDTO();
        mockResponse.setProfile(profile);
        mockResponse.setEducation(List.of());
        mockResponse.setExperience(List.of());
        mockResponse.setSkills(List.of());


        Mockito.when(profileService.getCompleteProfile(9L))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.summary").value("Backend Developer"));
    }

    @Test
    void testGetMyProfile_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(roles = "CANDIDATE")
    void testCreateOrUpdateProfile() throws Exception {
        ProfileUpdateResponseDTO updated =
                new ProfileUpdateResponseDTO("success", "Profile updated successfully", 9L);

        Mockito.when(profileService.createOrUpdateProfile(eq(9L), any(ProfileRequestDTO.class)))
                .thenReturn(updated);

        String json = """
        {
          "summary": "Backend Developer",
          "visibility": 1,
          "fileUrl": "https://cdn.example.com/profile1.pdf"
        }
        """;

        mockMvc.perform(post("/api/profile")
                        .with(authentication(new UsernamePasswordAuthenticationToken("9", "password", List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))) // ✅ 手动注入新的 Authentication
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.profileId").value(9));
    }




    @Test
    void testCreateOrUpdateProfile_Unauthenticated() throws Exception {
        String json = """
                {
                  "fullName": "Alice Chen"
                }
                """;

        mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
