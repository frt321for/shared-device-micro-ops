package com.iot.ops.application.module.site.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sites")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 256)
    private String address;

    @Column(length = 128)
    private String building;

    @Column(length = 32)
    private String floor;

    private Double latitude;
    private Double longitude;

    @Column(length = 64)
    private String businessHours;

    @Column(nullable = false, length = 16)
    private String serviceLevel;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 64)
    private String contactName;

    @Column(length = 32)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "active";
        if (serviceLevel == null) serviceLevel = "standard";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
