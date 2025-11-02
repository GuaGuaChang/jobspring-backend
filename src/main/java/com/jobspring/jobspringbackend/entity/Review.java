package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "title", length = 128)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    private Integer rating; // 1-5

    @Column(nullable = false)
    private Integer status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_note", length = 255)
    private String reviewNote;

    @Column(name = "public_at")
    private LocalDateTime publicAt;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;
}