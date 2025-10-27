package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.CompanyDTO;
import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock 所有依赖的 Service
    @MockBean
    private JobService jobService;
    @MockBean private AdminService adminService;
    @MockBean private ReviewService reviewService;
    @MockBean private HrApplicationService hrApplicationService;
    @MockBean private CompanyService companyService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllJobStatus() throws Exception {
        // 模拟数据
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        Job job = new Job();
        job.setId(100L);
        job.setTitle("Java Developer");
        job.setCompany(company);
        job.setStatus(0);

        Mockito.when(jobService.getAllJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/admin/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java Developer"))
                .andExpect(jsonPath("$[0].company").value("Test Company"))
                .andExpect(jsonPath("$[0].status").value(0));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchJobs() throws Exception {
        Page<JobDTO> mockPage = new PageImpl<>(List.of(new JobDTO()));
        Mockito.when(adminService.searchJobs(anyString(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/search")
                        .param("keyword", "java"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllCompanies() throws Exception {
        Page<CompanyDTO> mockPage = new PageImpl<>(List.of(new CompanyDTO()));
        Mockito.when(companyService.getAllCompanies(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/company/list"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateCompany() throws Exception {
        CompanyDTO mockCompany = new CompanyDTO();
        mockCompany.setName("New Company");

        Mockito.when(companyService.createCompany(any(CompanyDTO.class))).thenReturn(mockCompany);

        String companyJson = "{\"name\": \"New Company\"}";

        mockMvc.perform(multipart("/api/admin/company/create")
                        .file(new MockMultipartFile(
                                "company",
                                "",
                                "application/json",
                                companyJson.getBytes()
                        ))
                        .file(new MockMultipartFile(
                                "logo",
                                "logo.png",
                                "image/png",
                                "fakeImageData".getBytes()
                        ))
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeactivateJob() throws Exception {
        mockMvc.perform(post("/api/admin/companies/1/jobs/2/invalid")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Mockito.verify(jobService).deactivateJob(1L, 2L);
    }
}
