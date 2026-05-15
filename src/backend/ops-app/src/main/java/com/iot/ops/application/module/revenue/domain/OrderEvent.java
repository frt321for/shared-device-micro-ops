package com.iot.ops.application.module.revenue.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;

    private Long siteId;

    private Long skuId;

    @Builder.Default
    private Integer quantity = 1;

    private BigDecimal amount;

    private String payMethod;

    @Builder.Default
    private String status = "completed";

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
