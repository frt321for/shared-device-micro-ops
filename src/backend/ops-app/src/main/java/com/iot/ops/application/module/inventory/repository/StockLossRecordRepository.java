package com.iot.ops.application.module.inventory.repository;

import com.iot.ops.application.module.inventory.domain.StockLossRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockLossRecordRepository extends JpaRepository<StockLossRecord, Long> {

    List<StockLossRecord> findByDeviceId(Long deviceId);
}
