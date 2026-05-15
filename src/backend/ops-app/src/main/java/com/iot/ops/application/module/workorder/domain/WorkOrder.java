package com.iot.ops.application.module.workorder.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNo;

    private String type;

    @Builder.Default
    @Column(nullable = false)
    private String status = "pending_assign";

    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 2;

    private Long deviceId;
    private Long siteId;
    private Long skuId;
    private String title;

    @Transient
    private String siteName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer expectedQty;
    private Integer actualQty;
    private Long assigneeId;

    @Column(columnDefinition = "TEXT")
    private String priorityReason;

    private LocalDateTime arrivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;

    private String reviewResult;

    @Column(columnDefinition = "TEXT")
    private String reviewRemark;

    private LocalDateTime closedAt;
    private Long closedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
