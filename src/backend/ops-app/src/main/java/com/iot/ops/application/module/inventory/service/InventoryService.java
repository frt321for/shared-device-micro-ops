package com.iot.ops.application.module.inventory.service;

import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.inventory.domain.StockLossRecord;
import com.iot.ops.application.module.inventory.domain.WarehouseStock;
import com.iot.ops.application.module.inventory.domain.WarehouseTransaction;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import com.iot.ops.application.module.inventory.repository.StockLossRecordRepository;
import com.iot.ops.application.module.inventory.repository.WarehouseStockRepository;
import com.iot.ops.application.module.inventory.repository.WarehouseTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final SkuRepository skuRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseTransactionRepository warehouseTransactionRepository;
    private final DeviceStockRepository deviceStockRepository;
    private final StockLossRecordRepository stockLossRecordRepository;

    // ==================== SKU ====================

    public Sku findSkuById(Long id) {
        return skuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sku not found: " + id));
    }

    public Page<Sku> findAllSkus(Pageable pageable, String search, String category) {
        if (search != null && !search.isBlank()) {
            return skuRepository.findByNameContainingIgnoreCase(search, pageable);
        }
        if (category != null && !category.isBlank()) {
            return skuRepository.findByCategory(category, pageable);
        }
        return skuRepository.findAll(pageable);
    }

    @Transactional
    public Sku createSku(Sku sku) {
        return skuRepository.save(sku);
    }

    @Transactional
    public Sku updateSku(Long id, Sku updated) {
        Sku existing = findSkuById(id);
        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setUnit(updated.getUnit());
        existing.setCostPrice(updated.getCostPrice());
        existing.setSellingPrice(updated.getSellingPrice());
        existing.setShelfLifeDays(updated.getShelfLifeDays());
        existing.setReorderPoint(updated.getReorderPoint());
        existing.setDescription(updated.getDescription());
        return skuRepository.save(existing);
    }

    @Transactional
    public void deleteSku(Long id) {
        Sku sku = findSkuById(id);
        sku.setDeletedAt(LocalDateTime.now());
        skuRepository.save(sku);
    }

    // ==================== Warehouse ====================

    public List<WarehouseStock> getAllStock() {
        return warehouseStockRepository.findAll();
    }

    public List<WarehouseStock> getStock(Long skuId) {
        return warehouseStockRepository.findBySkuId(skuId);
    }

    @Transactional
    public WarehouseTransaction inbound(Long skuId, Integer quantity, String batchNo, String operator) {
        List<WarehouseStock> stocks = warehouseStockRepository.findBySkuId(skuId);
        WarehouseStock stock;
        if (stocks.isEmpty()) {
            stock = WarehouseStock.builder()
                    .skuId(skuId)
                    .quantity(quantity)
                    .batchNo(batchNo)
                    .build();
        } else {
            stock = stocks.get(0);
            stock.setQuantity(stock.getQuantity() + quantity);
            if (batchNo != null) {
                stock.setBatchNo(batchNo);
            }
        }
        warehouseStockRepository.save(stock);

        WarehouseTransaction tx = WarehouseTransaction.builder()
                .skuId(skuId)
                .type("INBOUND")
                .quantity(quantity)
                .operator(operator)
                .build();
        return warehouseTransactionRepository.save(tx);
    }

    @Transactional
    public WarehouseTransaction outbound(Long skuId, Integer quantity, String referenceType, Long referenceId, String operator) {
        List<WarehouseStock> stocks = warehouseStockRepository.findBySkuId(skuId);
        if (stocks.isEmpty()) {
            throw new IllegalStateException("Insufficient warehouse stock for sku: " + skuId);
        }
        WarehouseStock stock = stocks.get(0);
        if (stock.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient warehouse stock for sku: " + skuId);
        }
        stock.setQuantity(stock.getQuantity() - quantity);
        warehouseStockRepository.save(stock);

        if ("device".equals(referenceType) && referenceId != null) {
            Optional<DeviceStock> devStockOpt = deviceStockRepository.findByDeviceIdAndSkuId(referenceId, skuId);
            if (devStockOpt.isPresent()) {
                DeviceStock devStock = devStockOpt.get();
                devStock.setQuantity(devStock.getQuantity() + quantity);
                deviceStockRepository.save(devStock);
            }
        }

        WarehouseTransaction tx = WarehouseTransaction.builder()
                .skuId(skuId)
                .type("OUTBOUND")
                .quantity(quantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .operator(operator)
                .build();
        return warehouseTransactionRepository.save(tx);
    }

    // ==================== Warehouse Check ====================

    @Transactional
    public WarehouseStock warehouseCheck(Long skuId, Integer quantity, String operator) {
        List<WarehouseStock> stocks = warehouseStockRepository.findBySkuId(skuId);
        WarehouseStock stock;
        if (stocks.isEmpty()) {
            stock = WarehouseStock.builder()
                    .skuId(skuId)
                    .quantity(quantity)
                    .batchNo("MANUAL")
                    .build();
        } else {
            stock = stocks.get(0);
            stock.setQuantity(quantity);
        }
        warehouseStockRepository.save(stock);

        WarehouseTransaction tx = WarehouseTransaction.builder()
                .skuId(skuId)
                .type("CHECK")
                .quantity(quantity)
                .operator(operator)
                .build();
        warehouseTransactionRepository.save(tx);

        return stock;
    }

    // ==================== Device Stock ====================

    public List<DeviceStock> getDeviceStock(Long deviceId) {
        return deviceStockRepository.findByDeviceId(deviceId);
    }

    @Transactional
    public DeviceStock correctDeviceStock(Long id, Integer quantity, String reason, String operator) {
        DeviceStock ds = deviceStockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceStock not found: " + id));
        ds.setQuantity(quantity);
        ds.setCorrectedAt(LocalDateTime.now());
        ds.setCorrectedBy(operator + ": " + reason);
        ds.setStatus(determineStockStatus(quantity, ds.getMinThreshold(), ds.getMaxCapacity()));
        return deviceStockRepository.save(ds);
    }

    public List<DeviceStock> getPredictions() {
        return deviceStockRepository.findAll().stream()
                .filter(ds -> !"adequate".equals(ds.getStatus()))
                .toList();
    }

    private String determineStockStatus(int quantity, int minThreshold, int maxCapacity) {
        if (quantity <= 0) return "sold_out";
        if (quantity <= minThreshold) return "low";
        if (quantity <= maxCapacity * 0.2) return "almost_sold_out";
        return "adequate";
    }

    // ==================== Loss ====================

    @Transactional
    public StockLossRecord recordLoss(StockLossRecord record) {
        record.setId(null);
        StockLossRecord saved = stockLossRecordRepository.save(record);
        deviceStockRepository.findByDeviceIdAndSkuId(record.getDeviceId(), record.getSkuId())
                .ifPresent(ds -> {
                    ds.setQuantity(Math.max(0, ds.getQuantity() - record.getQuantity()));
                    deviceStockRepository.save(ds);
                });
        return saved;
    }

    public List<StockLossRecord> getLossRecords(Long deviceId) {
        if (deviceId != null) {
            return stockLossRecordRepository.findByDeviceId(deviceId);
        }
        return stockLossRecordRepository.findAll();
    }
}
