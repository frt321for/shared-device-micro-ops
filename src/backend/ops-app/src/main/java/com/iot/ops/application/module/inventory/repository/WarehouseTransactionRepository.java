package com.iot.ops.application.module.inventory.repository;

import com.iot.ops.application.module.inventory.domain.WarehouseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseTransactionRepository extends JpaRepository<WarehouseTransaction, Long> {

    List<WarehouseTransaction> findBySkuId(Long skuId);
}
