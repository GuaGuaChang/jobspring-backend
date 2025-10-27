package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.service.*;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobSeekerController.class)
class JobSeekerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JobService jobService;
    @MockBean private CompanyService companyService;
    @MockBean private ReviewService reviewService;
    @MockBean private UserRepository userRepository;
    @MockBean private JobseekerApplicationService jobseekerApplicationService;

    @Test
    @WithMockUser(username = "8", roles = "CANDIDATE")
    void testGetJobList() throws Exception {
        JobDTO job = new JobDTO();
        job.setId(1L);
        job.setTitle("Java Developer");
        Page<JobDTO> page = new PageImpl<>(List.of(job));

        Mockito.when(jobService.getJobSeekerJobs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job_seeker/job_list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "8", roles = "CANDIDATE")
    void testSearchJobs() throws Exception {
        JobDTO job = new JobDTO();
        job.setId(2L);
        job.setTitle("Backend Engineer");
        Page<JobDTO> page = new PageImpl<>(List.of(job));

        Mockito.when(jobService.searchJobSeekerJobs(eq("backend"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job_seeker/job_list/search")
                        .param("keyword", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));
    }

    @Test
    @WithMockUser(username = "8", roles = "CANDIDATE")
    void testGetCompany() throws Exception {
        CompanyDTO company = new CompanyDTO();
        company.setId(10L);
        company.setName("Google");

        Mockito.when(companyService.getCompanyById(10L))
                .thenReturn(company);

        mockMvc.perform(get("/api/job_seeker/company/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Google"));
    }

    @Test
    @WithMockUser(username = "5", roles = "CANDIDATE")
    void testPostReview() throws Exception {
        ReviewDTO review = new ReviewDTO();
        review.setId(99L);
        review.setContent("Great place to work!");

        Mockito.when(reviewService.createReview(any(JobSeekerReviewDTO.class), eq(5L)))
                .thenReturn(review);

        String json = """
                {
                  "companyId": 10,
                  "rating": 5,
                  "content": "Great place to work!"
                }
                """;

        mockMvc.perform(post("/api/job_seeker/postReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Great place to work!"));
    }

    @Test
    @WithMockUser(username = "8", roles = "CANDIDATE")
    void testJobseekerApplications() throws Exception {
        ApplicationBriefResponse app = new ApplicationBriefResponse();
        Page<ApplicationBriefResponse> page = new PageImpl<>(List.of(app));

        Mockito.when(jobseekerApplicationService.listMine(eq(8L), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job_seeker/applications"))
                .andExpect(status().isOk());
    }
}