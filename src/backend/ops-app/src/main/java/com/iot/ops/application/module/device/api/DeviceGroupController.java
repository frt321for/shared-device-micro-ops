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

    @GetMapping("/{id}")
    public ApiResponse<DeviceGroup> detail(@PathVariable Long id) {
        return ApiResponse.success(deviceGroupService.findById(id));
    }

    @PostMapping
    public ApiResponse<DeviceGroup> create(@Valid @RequestBody DeviceGroupRequest request) {
        return ApiResponse.created(deviceGroupService.create(request.name(), request.siteId(), request.description()));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceGroup> update(@PathVariable Long id, @Valid @RequestBody DeviceGroupRequest request) {
        return ApiResponse.success(deviceGroupService.update(id, request.name(), request.description()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceGroupService.delete(id);
        return ApiResponse.success(null);
    }

    // ==================== Members ====================

    @GetMapping("/{id}/members")
    public ApiResponse<List<com.iot.ops.application.module.device.domain.DeviceGroupMember>> listMembers(@PathVariable Long id) {
        return ApiResponse.success(deviceGroupService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(@PathVariable Long id, @RequestBody MemberRequest request) {
        deviceGroupService.addMember(id, request.deviceId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/members/{deviceId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long deviceId) {
        deviceGroupService.removeMember(id, deviceId);
        return ApiResponse.success(null);
    }

    public record DeviceGroupRequest(
        @jakarta.validation.constraints.NotBlank String name,
        @jakarta.validation.constraints.NotNull Long siteId,
        String description
    ) {}

    public record MemberRequest(
        @jakarta.validation.constraints.NotNull Long deviceId
    ) {}
}
