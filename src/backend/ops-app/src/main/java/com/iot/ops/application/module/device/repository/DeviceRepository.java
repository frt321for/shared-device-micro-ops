package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findBySiteId(Long siteId);
    List<Device> findByStatus(String status);
    List<Device> findByDeviceTypeId(Long deviceTypeId);
    long countBySiteIdAndStatus(Long siteId, String status);
}
