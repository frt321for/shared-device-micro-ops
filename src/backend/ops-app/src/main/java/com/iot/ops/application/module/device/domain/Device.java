package com.iot.ops.application.module.device.domain;

import com.iot.ops.common.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public void transitionTo(String newStatus) {
        Map<String, List<String>> allowed = Map.of(
            "inactive", List.of("online", "retired"),
            "online", List.of("fault", "low_stock", "retired"),
            "low_stock", List.of("out_of_stock", "online", "fault", "retired"),
            "out_of_stock", List.of("low_stock", "fault", "retired"),
            "fault", List.of("maintenance", "online", "retired"),
            "maintenance", List.of("online", "retired"),
            "recovering", List.of("online"),
            "retired", List.of()
        );
        List<String> next = allowed.getOrDefault(this.status, List.of());
        if (!next.contains(newStatus)) {
            throw new BusinessException("设备状态不允许转换: " + this.status + " → " + newStatus);
        }
        this.status = newStatus;
    }

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
