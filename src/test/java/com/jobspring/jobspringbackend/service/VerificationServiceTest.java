package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.entity.EmailVerificationCode;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.EmailVerificationCodeRepository;
import com.jobspring.jobspringbackend.util.CodeGenerator;
import com.jobspring.jobspringbackend.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private EmailVerificationCodeRepository repo;

    @Mock
    private MailService mail;

    @InjectMocks
    private VerificationService service;

    private EmailVerificationCode evc;
    private final String email = "test@example.com";


    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "dailyLimit", 3);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60);
        ReflectionTestUtils.setField(service, "expMinutes", 10);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);

        evc = new EmailVerificationCode();
        evc.setEmail(email);
        evc.setPurpose(VerificationService.PURPOSE_REGISTER);
        evc.setCodeHash(HashUtils.hash("123456"));
        evc.setStatus(0);
        evc.setAttemptCount(0);
        evc.setSentAt(LocalDateTime.now().minusMinutes(2));
        evc.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    // ========== sendRegisterCode ==========
    @Test
    void sendRegisterCode_shouldSendSuccessfully() {
        when(repo.countByEmailAndPurposeAndSentAtAfter(anyString(), anyString(), any())).thenReturn(0L);
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.empty());

        service.sendRegisterCode(email);

        verify(repo, times(1)).save(any(EmailVerificationCode.class));
        verify(mail, times(1)).sendPlainText(eq(email), contains("verification code"), anyString());
    }

    @Test
    void sendRegisterCode_shouldThrow_whenDailyLimitExceeded() {
        when(repo.countByEmailAndPurposeAndSentAtAfter(anyString(), anyString(), any())).thenReturn(3L);

        BizException ex = assertThrows(BizException.class, () ->
                service.sendRegisterCode(email)
        );

        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Too many requests"));
    }

    @Test
    void sendRegisterCode_shouldThrow_whenCooldownActive() {
        EmailVerificationCode recent = new EmailVerificationCode();
        recent.setSentAt(LocalDateTime.now());
        when(repo.countByEmailAndPurposeAndSentAtAfter(anyString(), anyString(), any())).thenReturn(0L);
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.of(recent));

        BizException ex = assertThrows(BizException.class, () ->
                service.sendRegisterCode(email)
        );

        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Please wait"));
    }

    // ========== verifyOrThrow ==========
    @Test
    void verifyOrThrow_shouldPass_whenCodeMatches() {
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.of(evc));

        service.verifyOrThrow(email, "123456");

        verify(repo, atLeast(1)).save(any(EmailVerificationCode.class));
        assertEquals(1, evc.getStatus());
        assertNotNull(evc.getVerifiedAt());
    }

    @Test
    void verifyOrThrow_shouldThrow_whenCodeWrong() {
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.of(evc));

        BizException ex = assertThrows(BizException.class, () ->
                service.verifyOrThrow(email, "999999")
        );

        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
        verify(repo, times(1)).save(any(EmailVerificationCode.class));
    }

    @Test
    void verifyOrThrow_shouldThrow_whenTooManyAttempts() {
        evc.setAttemptCount(5);
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.of(evc));

        BizException ex = assertThrows(BizException.class, () ->
                service.verifyOrThrow(email, "123456")
        );

        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getErrorCode());
        assertEquals(3, evc.getStatus()); // 被标记为 BLOCKED
        verify(repo).save(evc);
    }

    @Test
    void verifyOrThrow_shouldThrow_whenCodeNotFound() {
        when(repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () ->
                service.verifyOrThrow(email, "123456")
        );

        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Invalid or expired"));
    }
}
