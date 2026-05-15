package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {
    List<DeviceGroup> findBySiteId(Long siteId);
}
