package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.entity.EmailVerificationCode;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.EmailVerificationCodeRepository;
import com.jobspring.jobspringbackend.util.CodeGenerator;
import com.jobspring.jobspringbackend.util.HashUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VerificationService {

    public static final String PURPOSE_REGISTER = "REGISTER";

    private final EmailVerificationCodeRepository repo;
    private final MailService mail;

    @Value("${security.verification.expMinutes:10}")
    private int expMinutes;

    @Value("${security.verification.perEmailCooldownSeconds:60}")
    private int cooldownSeconds;

    @Value("${security.verification.dailyLimit:10}")
    private int dailyLimit;

    @Value("${security.verification.maxAttempts:5}")
    private int maxAttempts;

    public VerificationService(EmailVerificationCodeRepository repo, MailService mail) {
        this.repo = repo;
        this.mail = mail;
    }

    @Transactional
    public void sendRegisterCode(String email) {
        LocalDateTime now = LocalDateTime.now();

        long sentToday = repo.countByEmailAndPurposeAndSentAtAfter(email, PURPOSE_REGISTER, now.toLocalDate().atStartOfDay());
        if (sentToday >= dailyLimit) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Too many requests. Try again tomorrow.");
        }

        Optional<EmailVerificationCode> recentActive = repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(email, PURPOSE_REGISTER, 0, now);

        if (recentActive.isPresent() && recentActive.get().getSentAt().plusSeconds(cooldownSeconds).isAfter(now)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Please wait before requesting another code.");
        }

        String code = CodeGenerator.numeric6();
        String hash = HashUtils.hash(code);

        EmailVerificationCode evc = new EmailVerificationCode();
        evc.setEmail(email);
        evc.setPurpose(PURPOSE_REGISTER);
        evc.setCodeHash(hash);
        evc.setStatus(0);
        evc.setAttemptCount(0);
        evc.setSentAt(now);
        evc.setExpiresAt(now.plusMinutes(expMinutes));
        evc.setVerifiedAt(null);
        repo.save(evc);

        mail.sendPlainText(email, "Your JobSpring verification code", "Your verification code is: " + code + "\nIt expires in " + expMinutes + " minutes.");
    }

    @Transactional
    public void verifyOrThrow(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode evc = repo.findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(email, PURPOSE_REGISTER, 0, now).orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid or expired verification code."));

        if (evc.getAttemptCount() >= maxAttempts) {
            evc.setStatus(3); // BLOCKED
            repo.save(evc);
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Too many attempts. Please request a new code.");
        }

        evc.setAttemptCount(evc.getAttemptCount() + 1);

        if (!HashUtils.matches(code, evc.getCodeHash())) {
            repo.save(evc);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid or expired verification code.");
        }

        evc.setStatus(1); // USED
        evc.setVerifiedAt(now);
        repo.save(evc);
    }
}
