package com.iot.ops.application.module.inventory.service;

import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.inventory.domain.WarehouseStock;
import com.iot.ops.application.module.inventory.domain.WarehouseTransaction;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import com.iot.ops.application.module.inventory.repository.StockLossRecordRepository;
import com.iot.ops.application.module.inventory.repository.WarehouseStockRepository;
import com.iot.ops.application.module.inventory.repository.WarehouseTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void createSku_shouldSaveAndReturn() {
        Sku sku = Sku.builder().code("SKU001").name("Test Sku").unit("个").build();
        when(skuRepository.save(any(Sku.class))).thenReturn(sku);

        Sku result = inventoryService.createSku(sku);

        assertSame(sku, result);
        verify(skuRepository).save(sku);
    }

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
    void inbound_shouldIncreaseExistingStockQuantity() {
        WarehouseStock existing = WarehouseStock.builder().id(1L).skuId(1L).quantity(5).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(existing));
        when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseTransactionRepository.save(any(WarehouseTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.inbound(1L, 3, null, "op");

        assertEquals(8, existing.getQuantity());
    }

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
    void outbound_shouldThrowWhenStockIsInsufficient() {
        WarehouseStock stock = WarehouseStock.builder().id(1L).skuId(1L).quantity(2).build();
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of(stock));

        assertThrows(IllegalStateException.class,
                () -> inventoryService.outbound(1L, 5, "ORDER", 100L, "op"));
    }

    @Test
    void outbound_shouldThrowWhenNoStockExists() {
        when(warehouseStockRepository.findBySkuId(1L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> inventoryService.outbound(1L, 5, "ORDER", 100L, "op"));
    }
}
