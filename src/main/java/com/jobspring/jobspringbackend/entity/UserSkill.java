package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "user_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_user_skills",
                columnNames = {"user_id", "skill_id"}
        ))
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer level; // 1~5

    @Column(precision = 3, scale = 1)
    private BigDecimal years; // 年限
}