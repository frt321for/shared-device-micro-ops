package com.iot.ops.application.module.device.api;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceEvent;
import com.iot.ops.application.module.device.domain.DeviceTelemetry;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.repository.DeviceEventRepository;
import com.iot.ops.application.module.device.repository.TelemetryRepository;
import com.iot.ops.application.module.device.service.DeviceService;
import com.iot.ops.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final TelemetryRepository telemetryRepository;
    private final DeviceEventRepository deviceEventRepository;

    @GetMapping
    public ApiResponse<Page<Device>> list(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long deviceTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(deviceService.findAll(siteId, deviceTypeId, status, page, size));
    }

    @PostMapping
    public ApiResponse<Device> create(@Valid @RequestBody Device device) {
        return ApiResponse.created(deviceService.create(device));
    }

    @GetMapping("/{id}")
    public ApiResponse<Device> detail(@PathVariable Long id) {
        return deviceService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Device not found"));
    }

    @PutMapping("/{id}")
    public ApiResponse<Device> update(@PathVariable Long id, @Valid @RequestBody Device device) {
        return deviceService.findById(id)
                .map(existing -> {
                    device.setId(id);
                    return ApiResponse.success(deviceService.update(device));
                })
                .orElse(ApiResponse.error(404, "Device not found"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/telemetry")
    public ApiResponse<List<DeviceTelemetry>> telemetry(
            @PathVariable Long id,
            @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        if (start == null) start = Instant.now().minus(java.time.Duration.ofHours(24));
        if (end == null) end = Instant.now();
        return ApiResponse.success(telemetryRepository.findByDeviceIdAndMetricAndTimeBetweenOrderByTimeAsc(id, metric, start, end));
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<DeviceEvent>> events(@PathVariable Long id) {
        return ApiResponse.success(deviceEventRepository.findByDeviceIdOrderByOccurredAtDesc(id));
    }

    @GetMapping("/{id}/events/summary")
    public ApiResponse<Map<String, Long>> eventSummary(@PathVariable Long id) {
        List<DeviceEvent> events = deviceEventRepository.findByDeviceIdOrderByOccurredAtDesc(id);
        Map<String, Long> summary = new java.util.HashMap<>();
        for (DeviceEvent event : events) {
            summary.merge(event.getEventType(), 1L, Long::sum);
        }
        return ApiResponse.success(summary);
    }

}
