package com.iot.ops.application.module.inventory.repository;

import com.iot.ops.application.module.inventory.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    Optional<Sku> findByCode(String code);

    List<Sku> findByNameContainingIgnoreCase(String name);

    List<Sku> findByCategory(String category);
}
