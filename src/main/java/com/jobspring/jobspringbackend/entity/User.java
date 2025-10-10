package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_company_id", columnList = "company_id")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 128)
    private String fullName;

    @Column(nullable = false)
    private Integer role;  // 0=Candidate, 1=HR, 2=Admin

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    private Profile profile;

    // 新增：归属公司（可空；通常 HR 应该有值）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",
            foreignKey = @ForeignKey(name = "fk_users_company"))
    private Company company;
}
