package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobseekerApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private JobseekerApplicationService service;

    private User user;
    private Company company;
    private Job job;
    private Application app;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(10L);
        user.setFullName("Alice");

        company = new Company();
        company.setId(2L);
        company.setName("OpenAI SG");

        job = new Job();
        job.setId(3L);
        job.setTitle("Java Developer");
        job.setCompany(company);
        job.setLocation("Singapore");
        job.setEmploymentType(1);
        job.setStatus(0);

        app = new Application();
        app.setId(5L);
        app.setStatus(1);
        app.setAppliedAt(LocalDateTime.now());
        app.setResumeUrl("resume.pdf");
        app.setUser(user);
        app.setJob(job);
    }

    // ========= listMine(): status == null =========
    @Test
    void listMine_shouldCallFindMyApplications_whenStatusIsNull() {
        Page<Application> mockPage = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 5);

        when(applicationRepository.findMyApplications(10L, pageable))
                .thenReturn(mockPage);

        Page<ApplicationBriefResponse> result = service.listMine(10L, null, pageable);

        assertEquals(1, result.getTotalElements());
        ApplicationBriefResponse dto = result.getContent().get(0);
        assertEquals(5L, dto.getId());
        assertEquals("Java Developer", dto.getJobTitle());
        assertEquals("OpenAI SG", dto.getCompanyName());
        assertEquals(3L, dto.getJobId());
        assertEquals(2L, dto.getCompanyId());
        assertEquals("resume.pdf", dto.getResumeUrl());

        verify(applicationRepository).findMyApplications(10L, pageable);
        verify(applicationRepository, never()).findMyApplicationsByStatus(any(), any(), any());
    }

    // ========= listMine(): with status =========
    @Test
    void listMine_shouldCallFindMyApplicationsByStatus_whenStatusIsProvided() {
        Page<Application> mockPage = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(1, 5);

        when(applicationRepository.findMyApplicationsByStatus(10L, 1, pageable))
                .thenReturn(mockPage);

        Page<ApplicationBriefResponse> result = service.listMine(10L, 1, pageable);

        assertEquals(1, result.getTotalElements());
        verify(applicationRepository).findMyApplicationsByStatus(10L, 1, pageable);
    }

    // ========= listMine(): null job & company safe mapping =========
    @Test
    void listMine_shouldHandleNullJobAndCompanySafely() {
        Application a = new Application();
        a.setId(100L);
        a.setStatus(2);
        a.setAppliedAt(LocalDateTime.now());
        a.setResumeUrl("r.pdf");
        // job intentionally null

        Page<Application> mockPage = new PageImpl<>(List.of(a));
        Pageable pageable = PageRequest.of(0, 5);

        when(applicationRepository.findMyApplications(10L, pageable))
                .thenReturn(mockPage);

        Page<ApplicationBriefResponse> result = service.listMine(10L, null, pageable);

        ApplicationBriefResponse dto = result.getContent().get(0);
        assertEquals(100L, dto.getId());
        assertNull(dto.getJobId());
        assertNull(dto.getCompanyId());
        assertNull(dto.getCompanyName());
    }
}
