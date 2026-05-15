package com.iot.ops.application.module.inventory.api;

public record WarehouseOperationRequest(
        Long skuId,
        Integer quantity,
        String batchNo,
        String referenceType,
        Long referenceId,
        String operator
) {}
