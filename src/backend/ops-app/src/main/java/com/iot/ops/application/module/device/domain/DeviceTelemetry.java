package com.iot.ops.application.module.device.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "device_telemetry")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceTelemetry {

    @Id
    private Instant time;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String metric;

    private Double value;

    @Column(columnDefinition = "JSONB")
    private String tags;
}
