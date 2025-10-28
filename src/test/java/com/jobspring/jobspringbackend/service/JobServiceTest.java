package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private CompanyMemberRepository companyMemberRepository;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks
    private JobService service;

    private Company company;
    private Job job;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(10L);
        company.setName("OpenAI SG");

        job = new Job();
        job.setId(99L);
        job.setTitle("Java Developer");
        job.setLocation("Singapore");
        job.setEmploymentType(1);
        job.setSalaryMin(BigDecimal.valueOf(5000));
        job.setSalaryMax(BigDecimal.valueOf(10000));
        job.setDescription("Backend Developer");
        job.setStatus(0);
        job.setPostedAt(LocalDateTime.now());
        job.setCompany(company);
    }

    // ========== getAllJobs() ==========
    @Test
    void getAllJobs_shouldReturnAll() {
        when(jobRepository.findAll()).thenReturn(List.of(job));

        List<Job> result = service.getAllJobs();

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    // ========== getJobSeekerJobs() ==========
    @Test
    void getJobSeekerJobs_shouldMapToDTO() {
        Pageable pageable = PageRequest.of(0, 5);
        when(jobRepository.findByStatus(eq(0), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(job)));
        when(skillRepository.findSkillNamesByJobId(99L))
                .thenReturn(List.of("Spring Boot", "Java"));

        Page<JobDTO> result = service.getJobSeekerJobs(pageable);

        JobDTO dto = result.getContent().get(0);
        assertEquals("Java Developer", dto.getTitle());
        assertEquals("OpenAI SG", dto.getCompany());
        assertEquals(List.of("Spring Boot", "Java"), dto.getTags());
    }

    // ========== searchJobSeekerJobs() ==========
    @Test
    void searchJobSeekerJobs_shouldMapProperly() {
        Pageable pageable = PageRequest.of(1, 10);
        when(jobRepository.searchJobs(eq("Developer"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(job)));
        when(skillRepository.findSkillNamesByJobId(99L)).thenReturn(List.of("Kubernetes"));

        Page<JobDTO> result = service.searchJobSeekerJobs("Developer", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Developer", result.getContent().get(0).getTitle());
        assertEquals("Kubernetes", result.getContent().get(0).getTags().get(0));
    }

    // ========== createJob() ==========
    @Test
    void createJob_shouldCreateAndReturnResponse() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Backend Engineer");
        req.setLocation("Singapore");
        req.setEmploymentType(1);
        req.setSalaryMin(BigDecimal.valueOf(6000));
        req.setSalaryMax(BigDecimal.valueOf(12000));
        req.setDescription("Work on APIs");

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse res = service.createJob(10L, req);

        assertEquals("Backend Engineer", res.getTitle());
        assertEquals(10L, res.getCompanyId());
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void createJob_shouldThrow_whenSalaryMinGreaterThanMax() {
        JobCreateRequest req = new JobCreateRequest();
        req.setSalaryMin(BigDecimal.valueOf(20000));
        req.setSalaryMax(BigDecimal.valueOf(10000));
        assertThrows(IllegalArgumentException.class, () -> service.createJob(1L, req));
    }

    // ========== replaceJob() ==========
    @Test
    void replaceJob_shouldReplaceAndReturnNewJob() {
        JobUpdateRequest req = new JobUpdateRequest();
        req.setTitle("Senior Java Engineer");
        req.setDescription("Updated description");

        when(jobRepository.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse res = service.replaceJob(10L, 99L, req);

        assertEquals("Senior Java Engineer", res.getTitle());
        verify(jobRepository, times(2)).save(any(Job.class)); // old + new
    }

    @Test
    void replaceJob_shouldThrow_whenJobNotFound() {
        when(jobRepository.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () ->
                service.replaceJob(10L, 99L, new JobUpdateRequest()));
    }

    // ========== deactivateJob() ==========
    @Test
    void deactivateJob_shouldDeactivateAndPublishEvent() {
        when(jobRepository.findByIdAndCompanyId(99L, 10L))
                .thenReturn(Optional.of(job));

        service.deactivateJob(10L, 99L);

        assertEquals(1, job.getStatus());
        verify(publisher).publishEvent(any(JobService.JobDeactivatedEvent.class));
        verify(jobRepository).save(job);
    }

    @Test
    void deactivateJob_shouldThrow_whenNotFound() {
        when(jobRepository.findByIdAndCompanyId(anyLong(), anyLong())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.deactivateJob(10L, 123L));
    }

    // ========== listJobs() ==========
    @Test
    void listJobs_shouldCallFindByCompanyId_whenStatusNull() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findByCompanyId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(job)));

        Page<JobResponse> result = service.listJobs(10L, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(jobRepository).findByCompanyId(10L, pageable);
    }

    @Test
    void listJobs_shouldCallFindByCompanyIdAndStatus_whenStatusProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findByCompanyIdAndStatus(10L, 0, pageable))
                .thenReturn(new PageImpl<>(List.of(job)));

        Page<JobResponse> result = service.listJobs(10L, 0, pageable);

        assertEquals(1, result.getTotalElements());
        verify(jobRepository).findByCompanyIdAndStatus(10L, 0, pageable);
    }

    // ========== findCompanyIdByUserId() ==========
    @Test
    void findCompanyIdByUserId_shouldReturnCompanyId() {
        CompanyMember m = new CompanyMember();
        m.setCompany(company);
        when(companyMemberRepository.findFirstByUserIdAndRole(88L, "HR"))
                .thenReturn(Optional.of(m));

        Long result = service.findCompanyIdByUserId(88L);
        assertEquals(10L, result);
    }

    @Test
    void findCompanyIdByUserId_shouldThrow_whenNotFound() {
        when(companyMemberRepository.findFirstByUserIdAndRole(88L, "HR"))
                .thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.findCompanyIdByUserId(88L));
    }
}
