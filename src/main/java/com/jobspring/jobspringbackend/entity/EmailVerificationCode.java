package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification_codes",
        indexes = {
                @Index(name = "IDX_evc_email_purpose", columnList = "email,purpose"),
                @Index(name = "IDX_evc_status_expires", columnList = "status,expires_at")
        }
)
@Getter
@Setter
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 32)
    private String purpose; // REGISTER, RESET_PASSWORD...

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(nullable = false)
    private Integer status; // 0=ACTIVE,1=USED,2=EXPIRED,3=BLOCKED

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
