package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode>
    findFirstByEmailAndPurposeAndStatusAndExpiresAtAfterOrderBySentAtDesc(
            String email, String purpose, Integer status, LocalDateTime now);

    long countByEmailAndPurposeAndSentAtAfter(String email, String purpose, LocalDateTime after);
}
