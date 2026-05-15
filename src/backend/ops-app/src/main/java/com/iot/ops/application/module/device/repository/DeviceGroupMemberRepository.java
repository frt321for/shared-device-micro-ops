package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.DeviceGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceGroupMemberRepository extends JpaRepository<DeviceGroupMember, DeviceGroupMember.DeviceGroupMemberId> {
    long countByGroupId(Long groupId);
}
