package com.iot.ops.application.module.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_stock")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer minThreshold;

    @Column(nullable = false)
    private Integer maxCapacity;

    private LocalDateTime predictedSoldOut;

    @Column(nullable = false, length = 16)
    private String status;

    private LocalDateTime correctedAt;

    @Column(length = 64)
    private String correctedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "adequate";
        if (minThreshold == null) minThreshold = 10;
        if (maxCapacity == null) maxCapacity = 100;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
