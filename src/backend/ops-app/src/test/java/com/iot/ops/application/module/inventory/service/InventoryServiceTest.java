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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @Mock
    private WarehouseTransactionRepository warehouseTransactionRepository;

    @Mock
    private DeviceStockRepository deviceStockRepository;

    @Mock
    private StockLossRecordRepository stockLossRecordRepository;

    @InjectMocks
    private InventoryService inventoryService;

    // ==================== SKU ====================

    @Test
    void findSkuById_shouldReturnSku() {
        Sku sku = Sku.builder().id(1L).code("SKU001").name("Test Sku").unit("个").build();
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));

        Sku result = inventoryService.findSkuById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("SKU001", result.getCode());
        assertEquals("Test Sku", result.getName());
    }

    @Test
    void findSkuById_shouldThrowWhenNotFound() {
        when(skuRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> inventoryService.findSkuById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void createSku_shouldSaveAndReturn() {
        Sku sku = Sku.builder().code("SKU001").name("Test Sku").unit("个").build();
        when(skuRepository.save(any(Sku.class))).thenReturn(sku);

        Sku result = inventoryService.createSku(sku);

        assertSame(sku, result);
        verify(skuRepository).save(sku);
    }

    @Test
    void findAllSkus_shouldReturnAll_whenNoFilters() {
        Sku sku = Sku.builder().id(1L).code("SKU001").name("Test Sku").unit("个").build();
        Page<Sku> page = new PageImpl<>(List.of(sku));
        Pageable pageable = PageRequest.of(0, 10);
        when(skuRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Sku> result = inventoryService.findAllSkus(pageable, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("SKU001", result.getContent().get(0).getCode());
        verify(skuRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAllSkus_shouldFilterBySearch() {
        Sku sku = Sku.builder().id(1L).code("SKU001").name("Test Sku").unit("个").build();
        Page<Sku> page = new PageImpl<>(List.of(sku));
        Pageable pageable = PageRequest.of(0, 10);
        when(skuRepository.findByNameContainingIgnoreCase(eq("Test"), any(Pageable.class))).thenReturn(page);

        Page<Sku> result = inventoryService.findAllSkus(pageable, "Test", null);

        assertEquals(1, result.getTotalElements());
        assertEquals("SKU001", result.getContent().get(0).getCode());
        verify(skuRepository).findByNameContainingIgnoreCase(eq("Test"), any(Pageable.class));
    }

    @Test
    void findAllSkus_shouldFilterByCategory() {
        Sku sku = Sku.builder().id(1L).code("SKU001").name("Test Sku").category("电子").unit("个").build();
        Page<Sku> page = new PageImpl<>(List.of(sku));
        Pageable pageable = PageRequest.of(0, 10);
        when(skuRepository.findByCategory(eq("电子"), any(Pageable.class))).thenReturn(page);

        Page<Sku> result = inventoryService.findAllSkus(pageable, null, "电子");

        assertEquals(1, result.getTotalElements());
        assertEquals("电子", result.getContent().get(0).getCategory());
        verify(skuRepository).findByCategory(eq("电子"), any(Pageable.class));
    }

    @Test
    void updateSku_shouldUpdateFieldsAndSave() {
        Sku existing = Sku.builder().id(1L).code("SKU001").name("Old Name").unit("个").build();
        Sku input = Sku.builder()
                .code("SKU002").name("New Name").category("电子").unit("箱")
                .costPrice(BigDecimal.valueOf(10.00)).sellingPrice(BigDecimal.valueOf(15.50))
                .shelfLifeDays(30).reorderPoint(5).description("Updated desc")
                .build();

        when(skuRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sku result = inventoryService.updateSku(1L, input);

        assertEquals("SKU002", result.getCode());
        assertEquals("New Name", result.getName());
        assertEquals("电子", result.getCategory());
        assertEquals("箱", result.getUnit());
        assertEquals(BigDecimal.valueOf(10.00), result.getCostPrice());
        assertEquals(BigDecimal.valueOf(15.50), result.getSellingPrice());
        assertEquals(30, result.getShelfLifeDays());
        assertEquals(5, result.getReorderPoint());
        assertEquals("Updated desc", result.getDescription());
        verify(skuRepository).save(existing);
    }

    @Test
    void deleteSku_shouldSetDeletedAt() {
        Sku sku = Sku.builder().id(1L).code("SKU001").name("Test Sku").unit("个").build();
        when(skuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any(Sku.class))).thenReturn(sku);

        inventoryService.deleteSku(1L);

        assertNotNull(sku.getDeletedAt());
        assertTrue(sku.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(skuRepository).save(sku);
    }

    // ==================== Warehouse - Inbound ====================

    @Test
    void inbound_shouldCreateNewStockWhenNoneExists() {
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of());
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseTransaction tx = inventoryService.inbound(1L, 10, "BATCH001", "operator");

        assertEquals("INBOUND", tx.getType());
        assertEquals(10, tx.getQuantity());
        assertEquals("operator", tx.getOperator());
        verify(warehouseStockRepository).save(any(WarehouseStock.class));
    }

    @Test
    void inbound_shouldAddToExistingStock_whenExists() {
        WarehouseStock existing = WarehouseStock.builder().id(1L).skuId(1L).quantity(5).batchNo("OLD").build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(existing));
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseTransaction tx = inventoryService.inbound(1L, 3, "NEW_BATCH", "op");

        assertEquals("INBOUND", tx.getType());
        assertEquals(3, tx.getQuantity());
        assertEquals(8, existing.getQuantity());
        assertEquals("NEW_BATCH", existing.getBatchNo());
        verify(warehouseStockRepository).save(existing);
    }

    // ==================== Warehouse - Outbound ====================

    @Test
    void outbound_shouldDecreaseStockWhenSufficient() {
        WarehouseStock stock = WarehouseStock.builder().id(1L).skuId(1L).quantity(10).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(stock));
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseTransaction tx = inventoryService.outbound(1L, 3, "ORDER", 100L, "op");

        assertEquals("OUTBOUND", tx.getType());
        assertEquals(7, stock.getQuantity());
        assertEquals(100L, tx.getReferenceId());
    }

    @Test
    void outbound_shouldThrow_whenInsufficientStock() {
        WarehouseStock stock = WarehouseStock.builder().id(1L).skuId(1L).quantity(2).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(stock));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventoryService.outbound(1L, 5, "ORDER", 100L, "op"));
        assertTrue(ex.getMessage().contains("sku: " + 1L));
    }

    @Test
    void outbound_shouldThrow_whenStockNotFound() {
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventoryService.outbound(1L, 5, "ORDER", 100L, "op"));
        assertTrue(ex.getMessage().contains("sku: " + 1L));
    }

    @Test
    void outbound_shouldUpdateDeviceStock_whenDeviceReference() {
        WarehouseStock stock = WarehouseStock.builder().id(1L).skuId(1L).quantity(10).build();
        DeviceStock devStock = DeviceStock.builder().id(1L).deviceId(100L).skuId(1L).quantity(5).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(stock));
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenReturn(stock);
        when(deviceStockRepository.findByDeviceIdAndSkuId(100L, 1L)).thenReturn(Optional.of(devStock));
        when(deviceStockRepository.save(any(DeviceStock.class))).thenReturn(devStock);
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseTransaction tx = inventoryService.outbound(1L, 3, "device", 100L, "op");

        assertEquals("OUTBOUND", tx.getType());
        assertEquals(7, stock.getQuantity());
        assertEquals(8, devStock.getQuantity());
        verify(deviceStockRepository).save(devStock);
    }

    // ==================== Warehouse Check ====================

    @Test
    void warehouseCheck_shouldUpdateExistingStock() {
        WarehouseStock stock = WarehouseStock.builder().id(1L).skuId(1L).quantity(10).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(stock));
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseStock result = inventoryService.warehouseCheck(1L, 20, "op");

        assertEquals(20, result.getQuantity());
        verify(warehouseStockRepository).save(stock);
    }

    @Test
    void warehouseCheck_shouldCreateNewStock_whenNoneExists() {
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of());
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseStock result = inventoryService.warehouseCheck(1L, 20, "op");

        assertEquals(20, result.getQuantity());
        assertEquals("MANUAL", result.getBatchNo());
        verify(warehouseStockRepository).save(any(WarehouseStock.class));
    }

    // ==================== Device Stock ====================

    @Test
    void getDeviceStock_shouldReturnForDevice() {
        DeviceStock ds = DeviceStock.builder().id(1L).deviceId(1L).skuId(1L).quantity(10)
                .minThreshold(5).maxCapacity(100).status("adequate").build();
        when(deviceStockRepository.findByDeviceId(1L)).thenReturn(List.of(ds));

        List<DeviceStock> result = inventoryService.getDeviceStock(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDeviceId());
        assertEquals(10, result.get(0).getQuantity());
    }

    @Test
    void getDeviceStock_shouldReturnAll_whenDeviceIdNull() {
        DeviceStock ds1 = DeviceStock.builder().id(1L).deviceId(1L).quantity(10).minThreshold(5).maxCapacity(100).build();
        DeviceStock ds2 = DeviceStock.builder().id(2L).deviceId(2L).quantity(20).minThreshold(5).maxCapacity(100).build();
        when(deviceStockRepository.findAll()).thenReturn(List.of(ds1, ds2));

        List<DeviceStock> result = inventoryService.getDeviceStock(null);

        assertEquals(2, result.size());
        verify(deviceStockRepository).findAll();
    }

    @Test
    void correctDeviceStock_shouldUpdateAndSetStatus() {
        DeviceStock ds = DeviceStock.builder().id(1L).deviceId(1L).skuId(1L).quantity(10)
                .minThreshold(5).maxCapacity(100).status("adequate").build();
        when(deviceStockRepository.findById(1L)).thenReturn(Optional.of(ds));
        when(deviceStockRepository.save(any(DeviceStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceStock result = inventoryService.correctDeviceStock(1L, 50, "manual check", "op");

        assertEquals(50, result.getQuantity());
        assertEquals("adequate", result.getStatus());
        assertEquals("op: manual check", result.getCorrectedBy());
        assertNotNull(result.getCorrectedAt());
        verify(deviceStockRepository).save(ds);
    }

    @Test
    void correctDeviceStock_shouldThrow_whenNotFound() {
        when(deviceStockRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> inventoryService.correctDeviceStock(99L, 50, "reason", "op"));
        assertTrue(ex.getMessage().contains("99"));
    }

    // ==================== Loss Records ====================

    @Test
    void recordLoss_shouldSaveAndDecreaseDeviceStock() {
        StockLossRecord record = StockLossRecord.builder()
                .deviceId(1L).skuId(1L).quantity(3).reason("damaged").build();
        StockLossRecord saved = StockLossRecord.builder()
                .id(1L).deviceId(1L).skuId(1L).quantity(3).reason("damaged").build();
        DeviceStock devStock = DeviceStock.builder().id(1L).deviceId(1L).skuId(1L).quantity(10)
                .minThreshold(5).maxCapacity(100).build();

        when(stockLossRecordRepository.save(any(StockLossRecord.class))).thenReturn(saved);
        when(deviceStockRepository.findByDeviceIdAndSkuId(1L, 1L)).thenReturn(Optional.of(devStock));
        when(deviceStockRepository.save(any(DeviceStock.class))).thenReturn(devStock);

        StockLossRecord result = inventoryService.recordLoss(record);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(7, devStock.getQuantity());
        verify(stockLossRecordRepository).save(record);
        verify(deviceStockRepository).save(devStock);
    }

    @Test
    void getLossRecords_shouldReturnByDeviceId() {
        StockLossRecord rec = StockLossRecord.builder().id(1L).deviceId(1L).skuId(1L)
                .quantity(3).reason("damaged").build();
        when(stockLossRecordRepository.findByDeviceId(1L)).thenReturn(List.of(rec));

        List<StockLossRecord> result = inventoryService.getLossRecords(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDeviceId());
        assertEquals(3, result.get(0).getQuantity());
    }

    @Test
    void getLossRecords_shouldReturnAll_whenDeviceIdNull() {
        StockLossRecord rec1 = StockLossRecord.builder().id(1L).deviceId(1L).skuId(1L).quantity(3).reason("damaged").build();
        StockLossRecord rec2 = StockLossRecord.builder().id(2L).deviceId(2L).skuId(2L).quantity(5).reason("expired").build();
        when(stockLossRecordRepository.findAll()).thenReturn(List.of(rec1, rec2));

        List<StockLossRecord> result = inventoryService.getLossRecords(null);

        assertEquals(2, result.size());
        verify(stockLossRecordRepository).findAll();
    }
}
