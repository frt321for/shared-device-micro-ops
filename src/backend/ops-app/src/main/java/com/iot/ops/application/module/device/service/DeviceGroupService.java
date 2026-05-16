package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import com.iot.ops.application.module.device.domain.DeviceGroupMember;
import com.iot.ops.application.module.device.repository.DeviceGroupMemberRepository;
import com.iot.ops.application.module.device.repository.DeviceGroupRepository;
import com.iot.ops.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceGroupMemberRepository deviceGroupMemberRepository;

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

    public DeviceGroup update(Long id, String name, String description) {
        DeviceGroup group = findById(id);
        group.setName(name);
        group.setDescription(description);
        return deviceGroupRepository.save(group);
    }

    @Transactional
    public void delete(Long id) {
        DeviceGroup group = findById(id);
        deviceGroupMemberRepository.deleteByGroupId(id);
        deviceGroupRepository.delete(group);
    }

    public List<DeviceGroupMember> listMembers(Long groupId) {
        findById(groupId);
        return deviceGroupMemberRepository.findByGroupId(groupId);
    }

    public void addMember(Long groupId, Long deviceId) {
        findById(groupId);
        boolean exists = deviceGroupMemberRepository.findById(
            new DeviceGroupMember.DeviceGroupMemberId(groupId, deviceId)).isPresent();
        if (exists) return;
        DeviceGroupMember member = DeviceGroupMember.builder()
                .groupId(groupId)
                .deviceId(deviceId)
                .build();
        deviceGroupMemberRepository.save(member);
    }

    public void removeMember(Long groupId, Long deviceId) {
        findById(groupId);
        deviceGroupMemberRepository.deleteById(
            new DeviceGroupMember.DeviceGroupMemberId(groupId, deviceId));
    }
}
