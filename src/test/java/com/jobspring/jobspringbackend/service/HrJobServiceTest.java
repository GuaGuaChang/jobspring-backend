package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.HrJobResponse;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.exception.NotFoundException;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrJobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyMemberRepository companyMemberRepository;

    @InjectMocks
    private HrJobService service;

    private Company company;
    private Job job;
    private User hrUser;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(10L);
        company.setName("OpenAI SG");

        job = new Job();
        job.setId(100L);
        job.setCompany(company);
        job.setTitle("Java Developer");
        job.setLocation("Singapore");
        job.setEmploymentType(1);
        job.setSalaryMin(new BigDecimal("5000"));
        job.setSalaryMax(new BigDecimal("10000"));
        job.setDescription("Backend developer position");
        job.setStatus(0);
        job.setPostedAt(LocalDateTime.now());

        hrUser = new User();
        hrUser.setId(20L);
        hrUser.setCompany(company);
    }

    // ========== search() ==========

    @Test
    void search_success_withCompanyAndKeyword() {
        Page<Job> mockPage = new PageImpl<>(List.of(job));
        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.findWithCompanyById(20L))
                .thenReturn(Optional.of(hrUser));

        // ✅ 明确类型，避免歧义
        when(jobRepository.findAll(
                ArgumentMatchers.<Specification<Job>>any(),
                eq(pageable))
        ).thenReturn(mockPage);

        Page<HrJobResponse> result = service.search(20L, "Java Singapore", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Developer", result.getContent().get(0).title());

        verify(userRepository).findWithCompanyById(20L);
        verify(jobRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void search_shouldThrow_whenUserNotFound() {
        when(userRepository.findWithCompanyById(99L)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 5);

        assertThrows(EntityNotFoundException.class,
                () -> service.search(99L, null, pageable));
    }

    @Test
    void search_shouldThrow_whenUserHasNoCompany() {
        hrUser.setCompany(null);
        when(userRepository.findWithCompanyById(20L)).thenReturn(Optional.of(hrUser));
        Pageable pageable = PageRequest.of(0, 5);

        assertThrows(EntityNotFoundException.class,
                () -> service.search(20L, "test", pageable));
    }

    // ========== getJobForEdit() ==========

    @Test
    void getJobForEdit_success() {
        when(jobRepository.findByIdAndCompanyId(100L, 10L))
                .thenReturn(Optional.of(job));

        JobResponse r = service.getJobForEdit(10L, 100L);

        assertEquals(job.getId(), r.getId());
        assertEquals("Java Developer", r.getTitle());
        assertEquals(1, r.getEmploymentType());
        verify(jobRepository).findByIdAndCompanyId(100L, 10L);
    }

    @Test
    void getJobForEdit_shouldThrow_whenNotFound() {
        when(jobRepository.findByIdAndCompanyId(100L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getJobForEdit(10L, 100L));
    }

    // ========== findCompanyIdByUserId() ==========

    @Test
    void findCompanyIdByUserId_success() {
        when(companyMemberRepository.findCompanyIdByHrUserId(20L))
                .thenReturn(Optional.of(10L));

        Long result = service.findCompanyIdByUserId(20L);

        assertEquals(10L, result);
        verify(companyMemberRepository).findCompanyIdByHrUserId(20L);
    }

    @Test
    void findCompanyIdByUserId_shouldThrow_whenNoCompanyBound() {
        when(companyMemberRepository.findCompanyIdByHrUserId(20L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.findCompanyIdByUserId(20L));
    }

    // ========== 辅助函数 (日期/类型映射) 测试 ==========

    @Test
    void mapEmploymentType_shouldRecognizeFulltime() throws Exception {
        var method = HrJobService.class.getDeclaredMethod("mapEmploymentType", String.class);
        method.setAccessible(true);

        Integer r1 = (Integer) method.invoke(service, "fulltime");
        Integer r2 = (Integer) method.invoke(service, "intern");
        Integer r3 = (Integer) method.invoke(service, "合同工");
        Integer r4 = (Integer) method.invoke(service, "random");

        assertEquals(1, r1);
        assertEquals(2, r2);
        assertEquals(3, r3);
        assertNull(r4);
    }

    @Test
    void tryParseDateOrDateTime_shouldHandleDateAndDateTime() throws Exception {
        var method = HrJobService.class.getDeclaredMethod("tryParseDateOrDateTime", String.class);
        method.setAccessible(true);

        Object result1 = method.invoke(service, "2025-10-10");
        Object result2 = method.invoke(service, "2025-10-10T15:12:00");
        Object result3 = method.invoke(service, "invalid-date");

        assertNotNull(result1);
        assertNotNull(result2);
        assertNull(result3);
    }
}
