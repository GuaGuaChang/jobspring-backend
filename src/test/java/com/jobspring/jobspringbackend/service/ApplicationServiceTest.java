package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.dto.ApplicationDetailResponse;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.events.ApplicationSubmittedEvent;
import com.jobspring.jobspringbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


/**
 * 纯单元测试：不加载Spring；使用Mockito注入依赖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    @Mock JobRepository jobRepo;
    @Mock UserRepository userRepo;
    @Mock ApplicationRepository appRepo;
    @Mock CompanyMemberRepository memberRepo;
    @Mock HrCompanyService hrCompanyService; // 本类中未用到，但构造器需要
    @Mock ApplicationEventPublisher publisher;


    private Job jobValid;
    private Company company;
    private User userBasic;

    @InjectMocks
    private ApplicationService service;

    private ApplicationDTO dto;


    @BeforeEach
    void init() {
        company = new Company();
        company.setId(5L);
        company.setName("OpenAI SG");

        jobValid = new Job();
        jobValid.setId(10L);
        jobValid.setTitle("Java Dev");
        jobValid.setStatus(0);
        jobValid.setCompany(company);

        userBasic = new User();
        userBasic.setId(20L);
        userBasic.setFullName("Alice");
        userBasic.setEmail("alice@example.com");

        dto = new ApplicationDTO();
        dto.setResumeProfile("profile text");
    }

    @Test
    void apply_usesProfileFileUrl_whenPresent() {
        Profile profile = new Profile();
        profile.setFileUrl("https://cdn.example.com/resume.pdf");
        userBasic.setProfile(profile);

        lenient().when(jobRepo.findById(10L)).thenReturn(Optional.of(jobValid));
        lenient().when(userRepo.findById(20L)).thenReturn(Optional.of(userBasic));
        lenient().when(appRepo.existsByJobAndUser(jobValid, userBasic)).thenReturn(false);
        lenient().when(appRepo.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(888L);
            return a;
        });

        Long id = service.apply(10L, 20L, dto, null);

        assertEquals(888L, id);
        verify(appRepo).save(any(Application.class));
        verify(publisher).publishEvent(any(ApplicationSubmittedEvent.class));
    }


    @Test
    void apply_usesUploadedFileBase64_whenNoProfileFileUrl() throws Exception {
        // ✅ mock user
        User userBasic = mock(User.class);
        when(userBasic.getId()).thenReturn(20L);
        when(userBasic.getFullName()).thenReturn("Alice");
        when(userBasic.getEmail()).thenReturn("alice@example.com");
        when(userBasic.getProfile()).thenReturn(null);

        when(jobRepo.findById(10L)).thenReturn(Optional.of(jobValid));
        when(userRepo.findById(20L)).thenReturn(Optional.of(userBasic));
        when(appRepo.existsByJobAndUser(jobValid, userBasic)).thenReturn(false);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn("PDFDATA".getBytes(StandardCharsets.UTF_8));
        when(file.isEmpty()).thenReturn(false);

        when(appRepo.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(999L);
            return a;
        });

        ApplicationDTO dto = new ApplicationDTO();
        dto.setResumeProfile("hi");

        Long id = service.apply(10L, 20L, dto, file);

        assertEquals(999L, id);
        verify(appRepo).save(any(Application.class));
        verify(publisher).publishEvent(any(ApplicationSubmittedEvent.class));
    }



    @Test
    void apply_throws_whenNoResumeProvided() {
        // ✅ mock user
        User userBasic = mock(User.class);
        when(userBasic.getId()).thenReturn(20L);
        when(userBasic.getFullName()).thenReturn("Alice");
        when(userBasic.getEmail()).thenReturn("alice@example.com");
        when(userBasic.getProfile()).thenReturn(null);

        when(jobRepo.findById(10L)).thenReturn(Optional.of(jobValid));
        when(userRepo.findById(20L)).thenReturn(Optional.of(userBasic));
        when(appRepo.existsByJobAndUser(jobValid, userBasic)).thenReturn(false);

        // ✅ 执行 apply 并验证抛出异常
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.apply(10L, 20L, dto, null)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("no resume"));
        verify(appRepo, never()).save(any());
    }


    @Test
    void apply_throws_whenJobInactive() {
        Job inactive = new Job();
        inactive.setId(11L);
        inactive.setStatus(1); // 非0即无效
        when(jobRepo.findById(11L)).thenReturn(Optional.of(inactive));

        assertThrows(IllegalStateException.class,
                () -> service.apply(11L, 20L, dto("x"), null));
        verify(appRepo, never()).save(any());
    }

    // =============== apply: 已投递 -> 报错 =================

    @Test
    void apply_throws_whenAlreadyApplied() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(jobValid));
        when(userRepo.findById(20L)).thenReturn(Optional.of(userBasic));
        when(appRepo.existsByJobAndUser(jobValid, userBasic)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.apply(10L, 20L, dto("x"), null));
        verify(appRepo, never()).save(any());
    }

    // =============== detail: 公司匹配成功 =================

    @Test
    void getApplicationDetailForCompanyMember_success() {
        Application app = new Application();
        app.setId(700L);
        app.setJob(jobValid);
        app.setUser(userBasic);
        app.setStatus(0);
        app.setAppliedAt(LocalDateTime.now());
        app.setResumeUrl("u");
        app.setResumeProfile("p");

        // ✅ mock
        when(appRepo.findByIdWithJobAndCompany(700L))
                .thenReturn(Optional.of(app));
        when(memberRepo.findCompanyIdByHrUserId(20L))
                .thenReturn(Optional.of(5L)); // 与 jobValid.company.id 一致

        ApplicationDetailResponse r =
                service.getApplicationDetailForCompanyMember(20L, 700L);

        assertEquals(700L, r.getId());
        assertEquals("p", r.getResumeProfile());
        assertEquals("u", r.getResumeUrl());
        assertEquals(20L, r.getApplicantId());
    }


    @Test
    void getApplicationDetailForCompanyMember_denied_whenCompanyMismatch() {
        Application app = new Application();
        app.setId(701L);
        app.setJob(jobValid);
        app.setUser(userBasic);

        when(appRepo.findByIdWithJobAndCompany(701L))
                .thenReturn(Optional.of(app));
        when(memberRepo.findCompanyIdByHrUserId(20L))
                .thenReturn(Optional.of(999L)); // 与岗位公司不一致

        assertThrows(AccessDeniedException.class,
                () -> service.getApplicationDetailForCompanyMember(20L, 701L));
    }

    // =============== detail: 非HR或无绑定公司 -> 拒绝访问 =================

    @Test
    void getApplicationDetailForCompanyMember_denied_whenNotHr() {
        Application app = new Application();
        app.setId(702L);
        app.setJob(jobValid);
        app.setUser(userBasic);

        when(appRepo.findByIdWithJobAndCompany(702L))
                .thenReturn(Optional.of(app));
        when(memberRepo.findCompanyIdByHrUserId(20L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.getApplicationDetailForCompanyMember(20L, 702L));
    }

    // --------- helper ---------

    private ApplicationDTO dto(String profile) {
        ApplicationDTO d = new ApplicationDTO();
        d.setResumeProfile(profile);
        return d;
    }
}
