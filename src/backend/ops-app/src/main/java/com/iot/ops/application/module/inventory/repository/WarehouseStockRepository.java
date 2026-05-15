package com.iot.ops.application.module.inventory.repository;

import com.iot.ops.application.module.inventory.domain.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    List<WarehouseStock> findBySkuId(Long skuId);
}
