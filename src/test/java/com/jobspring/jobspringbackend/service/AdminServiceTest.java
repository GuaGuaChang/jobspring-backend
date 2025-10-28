package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.JobSearchResponse;
import com.jobspring.jobspringbackend.dto.PromoteToHrRequest;
import com.jobspring.jobspringbackend.dto.UserDTO;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.CompanyMember;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.repository.spec.UserSpecs;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock JobRepository jobRepository;
    @Mock SkillRepository skillRepository;
    @Mock UserRepository userRepository;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMemberRepository companyMemberRepository;

    @InjectMocks AdminService adminService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10, Sort.by("id").descending());
    }

    @Test
    void debugJob() {
        Job j = new Job();
        j.setId(1L);
        System.out.println(j.getId());
    }

    @Test
    void searchJobs_shouldMapFieldsAndTagsAndEmploymentType() {
        // job entity
        Company c = new Company();
        c.setId(7L);
        c.setName("OpenAI SG");

        Job j = new Job();
        j.setId(123L);
        j.setTitle("Java Developer");
        j.setLocation("Singapore");
        j.setSalaryMin(new BigDecimal("8000"));
        j.setSalaryMax(new BigDecimal("12000"));
        j.setPostedAt(LocalDateTime.now().minusDays(1));
        j.setEmploymentType(1); // Full-time
        j.setCompany(c);
        j.setDescription("Build cool stuff");

        when(jobRepository.adminSearchJobs(eq("java"), any()))
                .thenReturn(new PageImpl<>(List.of(j), pageable, 1));
        when(skillRepository.findSkillNamesByJobId(123L))
                .thenReturn(List.of("Spring", "Docker"));

        Page<JobDTO> page = adminService.searchJobs("java", pageable);

        assertEquals(1, page.getTotalElements());
        JobDTO dto = page.getContent().get(0);
        assertEquals(123L, dto.getId());
        assertEquals("Java Developer", dto.getTitle());
        assertEquals("Singapore", dto.getLocation());
        assertEquals(new BigDecimal("8000"), dto.getSalaryMin());
        assertEquals(new BigDecimal("12000"), dto.getSalaryMax());
        assertEquals("Full-time", dto.getEmploymentType()); // 映射正确
        assertEquals("OpenAI SG", dto.getCompany());
        assertEquals("Build cool stuff", dto.getDescription());
        assertEquals(List.of("Spring", "Docker"), dto.getTags());
    }


    @Test
    void makeHr_shouldPromoteCandidate_bindToProvidedCompany_andUpsertMembership() {
        User u = new User();
        u.setId(100L);
        u.setRole(0); // candidate
        u.setIsActive(true);
        Company oldCo = new Company();
        oldCo.setId(1L);
        oldCo.setName("OldCo");
        u.setCompany(oldCo);

        Company newCo = new Company();
        newCo.setId(2L);
        newCo.setName("NewCo");

        PromoteToHrRequest req = mock(PromoteToHrRequest.class);
        when(req.getCompanyId()).thenReturn(2L);
        when(req.getOverwriteCompany()).thenReturn(true);

        when(userRepository.findById(100L)).thenReturn(Optional.of(u));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(newCo));
        when(companyMemberRepository.findFirstByUserIdAndRole(100L, "HR"))
                .thenReturn(Optional.empty());

        adminService.makeHr(100L, req);

        // 角色升级
        assertEquals(1, u.getRole()); // ROLE_HR
        // 用户绑定到新公司
        assertEquals(2L, u.getCompany().getId());

        // 成员记录被创建并指向新公司
        ArgumentCaptor<CompanyMember> cmCap = ArgumentCaptor.forClass(CompanyMember.class);
        verify(companyMemberRepository).save(cmCap.capture());
        CompanyMember saved = cmCap.getValue();
        assertEquals(100L, saved.getUser().getId());
        assertEquals("HR", saved.getRole());
        assertEquals(2L, saved.getCompany().getId());

        verify(userRepository).save(u);
    }

    // ---------- makeHr：用户非激活 ----------

    @Test
    void makeHr_shouldThrow_ifUserInactive() {
        User u = new User();
        u.setId(200L);
        u.setRole(0);
        u.setIsActive(false);

        when(userRepository.findById(200L)).thenReturn(Optional.of(u));

        assertThrows(IllegalStateException.class,
                () -> adminService.makeHr(200L, null));
        verify(userRepository, never()).save(any());
    }

    // ---------- makeHr：Admin不可更改 ----------

    @Test
    void makeHr_shouldThrow_ifUserIsAdmin() {
        User u = new User();
        u.setId(201L);
        u.setRole(2); // ADMIN
        u.setIsActive(true);

        when(userRepository.findById(201L)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class,
                () -> adminService.makeHr(201L, null));
        verify(userRepository, never()).save(any());
    }

    // ---------- makeHr：不覆盖公司（overwrite=false）时保持原公司 ----------

    @Test
    void makeHr_shouldNotOverwriteCompany_whenOverwriteFalse_andExistingMembership() {
        // 用户已有公司A与成员记录
        Company companyA = new Company(); companyA.setId(10L); companyA.setName("A");
        Company companyB = new Company(); companyB.setId(11L); companyB.setName("B");

        User u = new User();
        u.setId(300L);
        u.setRole(0);
        u.setIsActive(true);
        u.setCompany(companyA);

        CompanyMember existing = new CompanyMember();
        existing.setId(501L);
        existing.setUser(u);
        existing.setCompany(companyA);
        existing.setRole("HR");

        PromoteToHrRequest req = mock(PromoteToHrRequest.class);
        when(req.getCompanyId()).thenReturn(11L);          // 指向B
        when(req.getOverwriteCompany()).thenReturn(false); // 不覆盖

        when(userRepository.findById(300L)).thenReturn(Optional.of(u));
        when(companyRepository.findById(11L)).thenReturn(Optional.of(companyB));
        when(companyMemberRepository.findFirstByUserIdAndRole(300L, "HR"))
                .thenReturn(Optional.of(existing));

        adminService.makeHr(300L, req);

        // 角色变HR，但仍保持原公司A
        assertEquals(1, u.getRole());
        assertEquals(10L, u.getCompany().getId());

        // 成员记录不被改到B公司
        verify(companyMemberRepository, never()).save(argThat(cm ->
                cm.getId() != null && cm.getCompany().getId() == 11L));
        // 但整体流程会保存用户
        verify(userRepository).save(u);
    }

    // ---------- searchUsers：q为空时查全部 ----------

    @Test
    void searchUsers_whenBlank_returnsAllPaged() {
        User u = new User(); u.setId(1L); u.setEmail("a@x.com"); u.setFullName("A");
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(u), pageable, 1));

        Page<UserDTO> page = adminService.searchUsers("  ", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(1L, page.getContent().get(0).getId());
        verify(userRepository, times(1)).findAll(pageable);
        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---------- searchUsers：q非空时用UserSpecs ----------

    @Test
    void searchUsers_whenNonBlank_usesUserSpecsFuzzySearch() {
        User u = new User(); u.setId(2L); u.setEmail("b@x.com"); u.setFullName("B");

        // 由于UserSpecs.fuzzySearch是静态工厂，这里只需要验证repository被以Spec形式调用即可
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(u), pageable, 1));

        Page<UserDTO> page = adminService.searchUsers("bee", pageable);

        assertEquals(1, page.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ---------- search：验证按Spec查询并正确映射 ----------

    @Test
    void search_shouldDelegateToRepositoryWithSpecification_andMapResponse() {
        Company co = new Company(); co.setId(9L); co.setName("Nine");
        Job j = new Job();
        j.setId(99L);
        j.setCompany(co);
        j.setTitle("Intern Java");
        j.setLocation("Shanghai");
        j.setEmploymentType(2);
        j.setSalaryMin(new BigDecimal("1000"));
        j.setSalaryMax(new BigDecimal("3000"));
        j.setStatus(0);
        j.setPostedAt(LocalDateTime.now());

        when(jobRepository.findAll(ArgumentMatchers.<Specification<Job>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(j), pageable, 1));


        Page<JobSearchResponse> page = adminService.search("java 2025-10-10", pageable);

        assertEquals(1, page.getTotalElements());
        JobSearchResponse r = page.getContent().get(0);
        assertEquals(99L, r.id());
        assertEquals(9L, r.companyId());
        assertEquals("Nine", r.companyName());
        assertEquals("Intern Java", r.title());
        assertEquals("Shanghai", r.location());
        assertEquals(2, r.employmentType());
        assertEquals(new BigDecimal("1000"), r.salaryMin());
        assertEquals(new BigDecimal("3000"), r.salaryMax());
        assertEquals(0, r.status());

        // 至少验证以Specification形式调用过
        verify(jobRepository).findAll(ArgumentMatchers.<Specification<Job>>any(), eq(pageable));
    }

    // ---------- makeHr：用户不存在 ----------

    @Test
    void makeHr_shouldThrow_ifUserNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> adminService.makeHr(404L, null));
    }
}
