package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.dto.ApplicationDetailResponse;
import com.jobspring.jobspringbackend.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testApplyWithFile() throws Exception {
        Mockito.when(applicationService.apply(anyLong(), anyLong(), any(ApplicationDTO.class), any()))
                .thenReturn(99L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes()
        );

        MockMultipartFile form = new MockMultipartFile(
                "formField", "", "application/json",
                "{\"note\":\"My Resume\"}".getBytes()
        );

        mockMvc.perform(multipart("/api/applications/{jobId}/applications", 12L)
                        .file(file)
                        .param("note", "My Resume")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", URI.create("/api/applications/99").toString()));

        Mockito.verify(applicationService).apply(eq(12L), eq(1L), any(ApplicationDTO.class), any());
    }

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testGetApplicationDetail() throws Exception {
        ApplicationDetailResponse resp = new ApplicationDetailResponse();
        resp.setApplicantId(88L);
        resp.setJobTitle("Backend Developer");

        Mockito.when(applicationService.getApplicationDetailForCompanyMember(eq(5L), eq(88L)))
                .thenReturn(resp);

        mockMvc.perform(get("/api/applications/{applicationId}", 88L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicantId").value(88))
                .andExpect(jsonPath("$.jobTitle").value("Backend Developer"));
    }
}