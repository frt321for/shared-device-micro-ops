package com.iot.ops.application.module.device.repository;

import com.iot.ops.application.module.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceCode(String deviceCode);
    List<Device> findBySiteId(Long siteId);
    List<Device> findByStatus(String status);
    List<Device> findByDeviceTypeId(Long deviceTypeId);
    long countBySiteIdAndStatus(Long siteId, String status);
    long countByStatus(String status);

    @Query("SELECT d.status, COUNT(d) FROM Device d GROUP BY d.status")
    List<Object[]> countByStatusGroup();
}
