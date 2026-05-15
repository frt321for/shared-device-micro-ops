package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.DeviceTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<DeviceTelemetry, Instant> {
    List<DeviceTelemetry> findByDeviceIdAndMetricAndTimeBetweenOrderByTimeAsc(
            Long deviceId, String metric, Instant start, Instant end);
}
