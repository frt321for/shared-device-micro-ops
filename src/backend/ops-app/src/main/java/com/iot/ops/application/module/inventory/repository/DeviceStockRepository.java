package com.iot.ops.application.module.inventory.repository;

import com.iot.ops.application.module.inventory.domain.DeviceStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceStockRepository extends JpaRepository<DeviceStock, Long> {

    List<DeviceStock> findByDeviceId(Long deviceId);

    Optional<DeviceStock> findByDeviceIdAndSkuId(Long deviceId, Long skuId);

    List<DeviceStock> findByStatus(String status);

    List<DeviceStock> findByQuantityLessThan(Integer threshold);
}
