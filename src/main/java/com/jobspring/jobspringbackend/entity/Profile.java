package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    private String summary;

    @Column(nullable = false)
    private Integer visibility = 0; // 0=私有 1=公司可见 2=公开

    @Column(name = "file_url", length = 512)
    private String fileUrl;
}