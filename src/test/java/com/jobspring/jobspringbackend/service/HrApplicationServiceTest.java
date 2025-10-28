package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private HrCompanyService hrCompanyService;

    @InjectMocks
    private HrApplicationService service;

    private Application app;
    private Job job;
    private Company company;
    private User user;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(10L);
        company.setName("OpenAI SG");

        job = new Job();
        job.setId(20L);
        job.setTitle("Backend Engineer");
        job.setCompany(company);
        job.setStatus(0);

        user = new User();
        user.setId(30L);
        user.setFullName("Alice");

        app = new Application();
        app.setId(100L);
        app.setJob(job);
        app.setUser(user);
        app.setStatus(1);
        app.setAppliedAt(LocalDateTime.now());
        app.setResumeUrl("https://cdn.example.com/resume.pdf");
    }

    // ========== listCompanyApplications ==========

    @Test
    void listCompanyApplications_shouldUseInferredCompanyId_whenCompanyIdNull() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Application> mockPage = new PageImpl<>(List.of(app));

        when(hrCompanyService.findCompanyIdByUserId(1L)).thenReturn(10L);
        when(applicationRepository.searchByCompany(10L, null, null, pageable))
                .thenReturn(mockPage);

        Page<ApplicationBriefResponse> result =
                service.listCompanyApplications(1L, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(app.getId(), result.getContent().get(0).getId());
        assertEquals("Backend Engineer", result.getContent().get(0).getJobTitle());
        verify(hrCompanyService).findCompanyIdByUserId(1L);
        verify(applicationRepository).searchByCompany(10L, null, null, pageable);
    }

    @Test
    void listCompanyApplications_shouldValidateHrBelongsToCompany_whenCompanyIdProvided() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Application> mockPage = new PageImpl<>(List.of(app));

        doNothing().when(hrCompanyService).assertHrInCompany(1L, 10L);
        when(applicationRepository.searchByCompany(10L, null, null, pageable))
                .thenReturn(mockPage);

        Page<ApplicationBriefResponse> result =
                service.listCompanyApplications(1L, 10L, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(hrCompanyService).assertHrInCompany(1L, 10L);
        verify(applicationRepository).searchByCompany(10L, null, null, pageable);
    }

    // ========== updateStatus ==========

    @Test
    void updateStatus_success() {
        when(applicationRepository.findByIdWithJobAndCompany(100L))
                .thenReturn(Optional.of(app));
        when(hrCompanyService.findCompanyIdByUserId(1L))
                .thenReturn(10L);

        ApplicationBriefResponse result = service.updateStatus(1L, 100L, 3);

        assertEquals(3, result.getStatus());
        assertEquals(app.getId(), result.getId());
        assertEquals("Backend Engineer", result.getJobTitle());
        verify(applicationRepository).findByIdWithJobAndCompany(100L);
    }

    @Test
    void updateStatus_shouldThrow_whenInvalidStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, 100L, 99));
    }

    @Test
    void updateStatus_shouldThrow_whenApplicationNotFound() {
        when(applicationRepository.findByIdWithJobAndCompany(100L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.updateStatus(1L, 100L, 2));
    }

    @Test
    void updateStatus_shouldThrow_whenHrNotFromSameCompany() {
        when(applicationRepository.findByIdWithJobAndCompany(100L))
                .thenReturn(Optional.of(app));
        when(hrCompanyService.findCompanyIdByUserId(1L)).thenReturn(999L);

        assertThrows(AccessDeniedException.class,
                () -> service.updateStatus(1L, 100L, 2));
    }

    @Test
    void updateStatus_shouldThrow_whenJobInactive() {
        job.setStatus(9);
        when(applicationRepository.findByIdWithJobAndCompany(100L))
                .thenReturn(Optional.of(app));
        when(hrCompanyService.findCompanyIdByUserId(1L)).thenReturn(10L);

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(1L, 100L, 2));
    }
}
