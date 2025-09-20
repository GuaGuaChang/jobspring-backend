package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false)
    private Integer status = 0; // 0 = 已投递，1 = 筛选中，2 = 约面，3 = 面试中，4 = 通过，5 = 拒绝，6 = 撤回

    private LocalDateTime appliedAt;

    @Lob
    private String resumeProfile;

    @Column(length = 1000)
    private String resumeUrl;
}