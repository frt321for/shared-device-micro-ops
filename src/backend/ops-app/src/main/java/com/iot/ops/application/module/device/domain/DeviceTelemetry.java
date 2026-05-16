package com.iot.ops.application.module.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_telemetry")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceTelemetry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String metric;

    private Double value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tags;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (time == null) time = Instant.now();
    }
}
