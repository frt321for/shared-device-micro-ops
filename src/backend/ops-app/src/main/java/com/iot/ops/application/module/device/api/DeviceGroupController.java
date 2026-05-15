package com.iot.ops.application.module.device.api;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import com.iot.ops.application.module.device.service.DeviceGroupService;
import com.iot.ops.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-groups")
@RequiredArgsConstructor
public class DeviceGroupController {

    private final DeviceGroupService deviceGroupService;

    @GetMapping
    public ApiResponse<List<DeviceGroup>> list(@RequestParam(required = false) Long siteId) {
        return ApiResponse.success(deviceGroupService.findAll(siteId));
    }

    @PostMapping
    public ApiResponse<DeviceGroup> create(@Valid @RequestBody DeviceGroupRequest request) {
        return ApiResponse.created(deviceGroupService.create(request.name(), request.siteId(), request.description()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceGroupService.delete(id);
        return ApiResponse.success(null);
    }

    public record DeviceGroupRequest(
        @jakarta.validation.constraints.NotBlank String name,
        @jakarta.validation.constraints.NotNull Long siteId,
        String description
    ) {}
}
