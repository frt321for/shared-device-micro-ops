package com.iot.ops.application.module.device.api;

import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.service.DeviceService;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceService deviceService;

    @GetMapping
    public ApiResponse<List<DeviceType>> list() {
        return ApiResponse.success(deviceService.getDeviceTypes());
    }
}
