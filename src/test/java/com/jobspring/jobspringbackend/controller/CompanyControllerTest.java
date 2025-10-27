package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.CompanyReviewDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private CompanyService companyService;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testCreateCompany() throws Exception {
        Company req = new Company();
        req.setId(1L);
        req.setName("Test Company");

        Mockito.when(companyRepository.save(any(Company.class))).thenReturn(req);

        String companyJson = """
                {
                  "name": "Test Company",
                  "description": "A software company"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Company"));

        Mockito.verify(companyRepository).save(any(Company.class));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testListCompanyJobs() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(11L);
        job.setTitle("Java Developer");

        Page<JobResponse> page = new PageImpl<>(List.of(job));
        Mockito.when(companyService.listCompanyJobs(anyLong(), anyInt(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/companies/{companyId}/jobs", 1L)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetCompanyReviews() throws Exception {
        CompanyReviewDTO review = new CompanyReviewDTO();
        review.setCompanyId(101L);
        review.setContent("Great company!");

        Page<CompanyReviewDTO> page = new PageImpl<>(List.of(review));
        Mockito.when(companyService.getCompanyReviews(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/companies/{companyId}/reviews", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Great company!"));
    }
}