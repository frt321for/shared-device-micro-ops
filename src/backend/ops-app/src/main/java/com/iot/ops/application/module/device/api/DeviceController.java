package com.iot.ops.application.module.device.api;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.service.DeviceService;
import com.iot.ops.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

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

}
