package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.dto.HrJobResponse;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.service.HrApplicationService;
import com.jobspring.jobspringbackend.service.HrCompanyService;
import com.jobspring.jobspringbackend.service.HrJobService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HrController.class)
class HrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HrApplicationService hrApplicationService;

    @MockBean
    private HrCompanyService hrCompanyService;

    @MockBean
    private HrJobService hrJobService;

    @Test
    @WithMockUser(username = "8", roles = "HR")
    void testListMineApplications() throws Exception {
        ApplicationBriefResponse app = new ApplicationBriefResponse();
        app.setId(11L);
        app.setJobTitle("Java Developer");

        Page<ApplicationBriefResponse> mockPage = new PageImpl<>(List.of(app));
        Mockito.when(hrApplicationService.listCompanyApplications(eq(8L), isNull(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/api/hr/applications")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobTitle").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "9", roles = "HR")
    void testGetMyCompanyId() throws Exception {
        Mockito.when(hrCompanyService.getCompanyIdOfHr(9L)).thenReturn(77L);

        mockMvc.perform(get("/api/hr/company-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(77));
    }

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testUpdateStatus() throws Exception {
        ApplicationBriefResponse updated = new ApplicationBriefResponse();
        updated.setId(44L);
        updated.setStatus(2); // 假设 2 = 已通过

        Mockito.when(hrApplicationService.updateStatus(eq(5L), eq(44L), eq(2)))
                .thenReturn(updated);

        String jsonBody = """
                {
                  "status": 2
                }
                """;

        mockMvc.perform(post("/api/hr/applications/{applicationId}/status", 44L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2));
    }

    @Test
    @WithMockUser(username = "3", roles = "HR")
    void testMyCompanyName() throws Exception {
        Mockito.when(hrCompanyService.getMyCompanyName(3L)).thenReturn("SuperTech");

        mockMvc.perform(get("/api/hr/company-name"))
                .andExpect(status().isOk())
                .andExpect(content().string("SuperTech"));
    }

    @Test
    @WithMockUser(username = "10", roles = "HR")
    void testSearchJobs() throws Exception {
        HrJobResponse job = new HrJobResponse(
                100L,
                "Backend Engineer",
                "Singapore",
                1,
                new BigDecimal("5000"),
                new BigDecimal("8000"),
                1,
                LocalDateTime.now()
        );

        Page<HrJobResponse> page = new PageImpl<>(List.of(job));

        Mockito.when(hrJobService.search(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/hr/jobs")
                        .param("q", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));
    }


    @Test
    @WithMockUser(username = "7", roles = "HR")
    void testGetJobDetailForEdit() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(200L);
        job.setTitle("Data Engineer");

        Mockito.when(hrJobService.findCompanyIdByUserId(7L)).thenReturn(15L);
        Mockito.when(hrJobService.getJobForEdit(15L, 200L)).thenReturn(job);

        mockMvc.perform(get("/api/hr/jobs-detail/{jobId}", 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Data Engineer"));
    }
}