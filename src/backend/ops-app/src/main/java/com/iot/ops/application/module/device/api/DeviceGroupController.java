package com.iot.ops.application.module.device.api;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import com.iot.ops.application.module.device.service.DeviceGroupService;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<DeviceGroup> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long siteId = body.get("siteId") != null ? ((Number) body.get("siteId")).longValue() : null;
        String description = (String) body.get("description");

        if (name == null || siteId == null) {
            return ApiResponse.error(400, "name and siteId are required");
        }

        return ApiResponse.created(deviceGroupService.create(name, siteId, description));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceGroupService.delete(id);
        return ApiResponse.success(null);
    }
}
