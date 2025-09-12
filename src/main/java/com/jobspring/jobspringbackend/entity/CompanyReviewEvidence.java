package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "company_review_evidences")
public class CompanyReviewEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(length = 128)
    private String title;

    @Lob
    private String description;

    private LocalDateTime createdAt;
}