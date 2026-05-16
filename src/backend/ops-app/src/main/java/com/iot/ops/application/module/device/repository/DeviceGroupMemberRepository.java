package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.DeviceGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceGroupMemberRepository extends JpaRepository<DeviceGroupMember, DeviceGroupMember.DeviceGroupMemberId> {
    long countByGroupId(Long groupId);
    List<DeviceGroupMember> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}
