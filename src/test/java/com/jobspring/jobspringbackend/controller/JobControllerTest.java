package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.JobCreateRequest;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.dto.JobUpdateRequest;
import com.jobspring.jobspringbackend.service.JobService;
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

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testCreateJob() throws Exception {
        JobResponse mockRes = new JobResponse();
        mockRes.setId(100L);
        mockRes.setTitle("New Java Developer");

        Mockito.when(jobService.createJob(eq(1L), any(JobCreateRequest.class)))
                .thenReturn(mockRes);

        String json = """
                {
                  "title": "New Java Developer",
                  "description": "Backend Java Role",
                  "employmentType": 1
                }
                """;


        mockMvc.perform(post("/api/hr/companies/{companyId}/jobs", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", URI.create("/api/hr/jobs/100").toString()))
                .andExpect(jsonPath("$.title").value("New Java Developer"));
    }

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testUpdateJob() throws Exception {
        JobResponse updated = new JobResponse();
        updated.setId(101L);
        updated.setTitle("Updated Job");

        Mockito.when(jobService.replaceJob(eq(1L), eq(100L), any(JobUpdateRequest.class)))
                .thenReturn(updated);

        String json = """
                {
                  "title": "Updated Job"
                }
                """;

        mockMvc.perform(patch("/api/hr/companies/{companyId}/jobs/{jobId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Job"));
    }

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testDeactivateJob() throws Exception {
        mockMvc.perform(post("/api/hr/companies/{companyId}/jobs/{jobId}/invalid", 1L, 100L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Mockito.verify(jobService).deactivateJob(1L, 100L);
    }

    @Test
    @WithMockUser(username = "5", roles = "HR")
    void testListJobs() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(200L);
        job.setTitle("Software Engineer");

        Page<JobResponse> page = new PageImpl<>(List.of(job));

        Mockito.when(jobService.findCompanyIdByUserId(5L)).thenReturn(1L);
        Mockito.when(jobService.listJobs(eq(1L), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/hr/companies/jobs")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Software Engineer"));
    }
}
