package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.CompanyDTO;
import com.jobspring.jobspringbackend.dto.CompanyReviewDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private CompanyService service;

    private Company company;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(1L);
        company.setName("OpenAI SG");
        company.setWebsite("https://openai.com");
        company.setDescription("AI Research");
        company.setSize(1000);
        company.setLogoUrl("logo.png");
        company.setCreatedBy("100");
    }

    // ========== getCompanyById ==========

    @Test
    void getCompanyById_found() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyDTO dto = service.getCompanyById(1L);

        assertEquals("OpenAI SG", dto.getName());
        assertEquals("AI Research", dto.getDescription());
        verify(companyRepository).findById(1L);
    }

    @Test
    void getCompanyById_notFound_throws() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getCompanyById(99L));
    }

    // ========== getAllCompanies ==========

    @Test
    void getAllCompanies_returnsPage() {
        Page<Company> mockPage = new PageImpl<>(List.of(company));
        when(companyRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<CompanyDTO> page = service.getAllCompanies(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("OpenAI SG", page.getContent().get(0).getName());
    }

    // ========== listCompanyJobs ==========

    @Test
    void listCompanyJobs_withStatus() {
        Job job = new Job();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setLocation("Singapore");
        job.setEmploymentType(1);
        job.setSalaryMin(BigDecimal.valueOf(5000));
        job.setSalaryMax(BigDecimal.valueOf(10000));
        job.setDescription("Develop AI tools");
        job.setStatus(0);
        job.setCompany(company);
        job.setPostedAt(LocalDateTime.now());

        Page<Job> mockPage = new PageImpl<>(List.of(job));
        when(jobRepository.findByCompanyIdAndStatus(1L, 0, PageRequest.of(0, 5)))
                .thenReturn(mockPage);

        Page<JobResponse> result = service.listCompanyJobs(1L, 0, PageRequest.of(0, 5));

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Developer", result.getContent().get(0).getTitle());
    }

    @Test
    void listCompanyJobs_withoutStatus() {
        Company company = new Company();
        company.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setCompany(company); // ✅ 必须加这行
        job.setStatus(0);

        Page<Job> mockPage = new PageImpl<>(List.of(job));

        when(jobRepository.findByCompanyId(1L, PageRequest.of(0, 5)))
                .thenReturn(mockPage);

        Page<JobResponse> result = service.listCompanyJobs(1L, null, PageRequest.of(0, 5));

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Developer", result.getContent().get(0).getTitle());
        verify(jobRepository).findByCompanyId(1L, PageRequest.of(0, 5));
    }


    @Test
    void getCompanyReviews_success() {
        Review review = new Review();
        review.setId(300L);
        review.setTitle("Good company");
        review.setContent("Great environment");
        review.setRating(5);
        review.setPublicAt(LocalDateTime.now());
        review.setImageUrl("img.png");
        review.setStatus(1);

        Application app = new Application();
        Job job = new Job();
        job.setCompany(company);
        app.setJob(job);
        review.setApplication(app);

        Page<Review> mockPage = new PageImpl<>(List.of(review));
        when(reviewRepository.findByCompanyId(1L, PageRequest.of(0, 10))).thenReturn(mockPage);

        Page<CompanyReviewDTO> result = service.getCompanyReviews(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Good company", result.getContent().get(0).getTitle());
        assertEquals(1L, result.getContent().get(0).getCompanyId());
    }

    // ========== createCompany ==========

    @Test
    void createCompany_success() {
        CompanyDTO dto = new CompanyDTO();
        dto.setName("ChronoFlow Ltd");
        dto.setWebsite("https://chrono.com");
        dto.setDescription("DevOps platform");
        dto.setCreatedBy("123");

        when(companyRepository.existsByName("ChronoFlow Ltd")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompanyDTO saved = service.createCompany(dto);

        assertEquals(10L, saved.getId());
        assertEquals("ChronoFlow Ltd", saved.getName());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void createCompany_conflict_throws() {
        CompanyDTO dto = new CompanyDTO();
        dto.setName("OpenAI SG");

        when(companyRepository.existsByName("OpenAI SG")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> service.createCompany(dto));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("already exists"));
    }
}
