package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_job_skills",
                columnNames = {"job_id", "skill_id"}
        ))
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(nullable = false)
    private Integer weight = 5; // 默认权重5（0-10）
}