package com.jobspring.jobspringbackend.entity;

import com.jobspring.jobspringbackend.constant.EmploymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private Integer employmentType = EmploymentType.FULL_TIME; // 1=全职 2=实习 3=合同工

    @Column(precision = 10, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 10, scale = 2)
    private BigDecimal salaryMax;

    @Lob
    private String description;

    @Column(nullable = false)
    private Integer status = 0; // 0=有效 1=无效

    private LocalDateTime postedAt;
}