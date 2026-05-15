package com.iot.ops.application.module.device.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(name = "device_type_id", nullable = false)
    private Long deviceTypeId;

    @Column(nullable = false, length = 128)
    private String name;

    private Long siteId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 64)
    private String model;

    private Integer capacity;

    @Column(length = 128)
    private String locationDesc;

    private LocalDate installDate;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    private LocalDateTime lastHeartbeat;

    private Integer inventoryWarnThreshold;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "offline";
        if (inventoryWarnThreshold == null) inventoryWarnThreshold = 20;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
