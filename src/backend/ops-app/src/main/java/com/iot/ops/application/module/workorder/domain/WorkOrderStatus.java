package com.iot.ops.application.module.workorder.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderStatus {

    PENDING_ASSIGN("待派单"),
    ASSIGNED("已派单"),
    ARRIVED("已到场"),
    PROCESSING("处理中"),
    PENDING_REVIEW("待复核"),
    CLOSED("已关闭"),
    REJECTED("已驳回"),
    CANCELLED("已取消");

    private final String displayName;
}
