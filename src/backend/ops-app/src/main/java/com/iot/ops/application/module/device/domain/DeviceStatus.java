package com.iot.ops.application.module.device.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceStatus {

    INACTIVE("未启用"),
    ONLINE("在线"),
    LOW_STOCK("缺货预警"),
    OUT_OF_STOCK("缺货"),
    FAULT("故障"),
    MAINTENANCE("维护中"),
    RECOVERING("恢复在线"),
    RETIRED("停用");

    private final String displayName;
}
