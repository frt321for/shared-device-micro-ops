package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import com.iot.ops.application.module.device.repository.DeviceGroupRepository;
import com.iot.ops.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;

    public List<DeviceGroup> findAll(Long siteId) {
        if (siteId != null) {
            return deviceGroupRepository.findBySiteId(siteId);
        }
        return deviceGroupRepository.findAll();
    }

    public DeviceGroup findById(Long id) {
        return deviceGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DeviceGroup not found: " + id));
    }

    public DeviceGroup create(String name, Long siteId, String description) {
        DeviceGroup group = DeviceGroup.builder()
                .name(name)
                .siteId(siteId)
                .description(description)
                .build();
        return deviceGroupRepository.save(group);
    }

    public void delete(Long id) {
        DeviceGroup group = findById(id);
        deviceGroupRepository.delete(group);
    }
}
