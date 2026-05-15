package com.iot.ops.application.module.inventory.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockStatus {

    ADEQUATE("充足"),
    LOW("低库存"),
    ALMOST_SOLD_OUT("即将售罄"),
    SOLD_OUT("已售罄"),
    PENDING_REPLENISH("待补货"),
    REPLENISHED("已补货");

    private final String displayName;
}
