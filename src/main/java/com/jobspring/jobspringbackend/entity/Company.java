package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    private Integer size; // 人数（估算）

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Lob
    private String description;

    @Column(name = "created_by", length = 255)
    private String createdBy;
}