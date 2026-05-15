package com.iot.ops.application.module.inventory.api;

import com.iot.ops.common.ApiResponse;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.inventory.domain.StockLossRecord;
import com.iot.ops.application.module.inventory.domain.WarehouseStock;
import com.iot.ops.application.module.inventory.domain.WarehouseTransaction;
import com.iot.ops.application.module.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== SKU ====================

    @GetMapping("/skus")
    public ApiResponse<Page<Sku>> listSkus(Pageable pageable,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(required = false) String category) {
        return ApiResponse.success(inventoryService.findAllSkus(pageable, search, category));
    }

    @GetMapping("/skus/{id}")
    public ApiResponse<Sku> getSku(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.findSkuById(id));
    }

    @PostMapping("/skus")
    public ApiResponse<Sku> createSku(@RequestBody Sku sku) {
        return ApiResponse.success(inventoryService.createSku(sku));
    }

    @PutMapping("/skus/{id}")
    public ApiResponse<Sku> updateSku(@PathVariable Long id, @RequestBody Sku sku) {
        return ApiResponse.success(inventoryService.updateSku(id, sku));
    }

    @DeleteMapping("/skus/{id}")
    public ApiResponse<Void> deleteSku(@PathVariable Long id) {
        inventoryService.deleteSku(id);
        return ApiResponse.success("Deleted successfully", null);
    }

    // ==================== Warehouse ====================

    @GetMapping("/warehouse/stock")
    public ApiResponse<List<WarehouseStock>> getAllWarehouseStock() {
        return ApiResponse.success(inventoryService.getAllStock());
    }

    @GetMapping("/warehouse/stock/{skuId}")
    public ApiResponse<List<WarehouseStock>> getWarehouseStock(@PathVariable Long skuId) {
        return ApiResponse.success(inventoryService.getStock(skuId));
    }

    @PostMapping("/warehouse/inbound")
    public ApiResponse<WarehouseTransaction> inbound(@RequestBody WarehouseOperationRequest request) {
        WarehouseTransaction tx = inventoryService.inbound(
                request.skuId(), request.quantity(), request.batchNo(), request.operator());
        return ApiResponse.success(tx);
    }

    @PostMapping("/warehouse/check")
    public ApiResponse<WarehouseStock> warehouseCheck(@RequestParam Long skuId,
                                                      @RequestParam Integer quantity,
                                                      @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success(inventoryService.warehouseCheck(skuId, quantity, operator));
    }

    @PostMapping("/warehouse/outbound")
    public ApiResponse<WarehouseTransaction> outbound(@RequestBody WarehouseOperationRequest request) {
        WarehouseTransaction tx = inventoryService.outbound(
                request.skuId(), request.quantity(),
                request.referenceType(), request.referenceId(), request.operator());
        return ApiResponse.success(tx);
    }

    // ==================== Device Stock ====================

    @GetMapping("/device-stock")
    public ApiResponse<List<DeviceStock>> getDeviceStock(@RequestParam Long deviceId) {
        return ApiResponse.success(inventoryService.getDeviceStock(deviceId));
    }

    @GetMapping("/device-stock/{deviceId}")
    public ApiResponse<List<DeviceStock>> getDeviceStockByDeviceId(@PathVariable Long deviceId) {
        return ApiResponse.success(inventoryService.getDeviceStock(deviceId));
    }

    @PutMapping("/device-stock/{id}/correct")
    public ApiResponse<DeviceStock> correctDeviceStock(@PathVariable Long id,
                                                       @RequestParam Integer quantity,
                                                       @RequestParam String reason,
                                                       @RequestParam String operator) {
        return ApiResponse.success(inventoryService.correctDeviceStock(id, quantity, reason, operator));
    }

    @GetMapping("/stock/predictions")
    public ApiResponse<List<DeviceStock>> getPredictions() {
        return ApiResponse.success(inventoryService.getPredictions());
    }

    // ==================== Loss ====================

    @GetMapping("/stock/loss")
    public ApiResponse<List<StockLossRecord>> getLossRecords(@RequestParam(required = false) Long deviceId) {
        return ApiResponse.success(inventoryService.getLossRecords(deviceId));
    }

    @PostMapping("/stock/loss")
    public ApiResponse<StockLossRecord> recordLoss(@RequestBody StockLossRecord record) {
        return ApiResponse.success(inventoryService.recordLoss(record));
    }
}
