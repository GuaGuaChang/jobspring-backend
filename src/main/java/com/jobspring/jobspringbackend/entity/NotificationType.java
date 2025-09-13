package com.jobspring.jobspringbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_types")
public class NotificationType {

    @Id
    @Column(length = 64)
    private String type; // 主键

    @Lob
    private String title;

    @Lob
    private String content;
}