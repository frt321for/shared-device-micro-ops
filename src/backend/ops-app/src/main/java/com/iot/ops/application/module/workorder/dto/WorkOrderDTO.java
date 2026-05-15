package com.iot.ops.application.module.workorder.dto;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class WorkOrderDTO {
    private Long id;
    private String orderNo;
    private String type;
    private String status;
    private Integer priority;
    private Long deviceId;
    private Long siteId;
    private String siteName;
    private Long skuId;
    private String title;
    private String description;
    private Integer expectedQty;
    private Integer actualQty;
    private Long assigneeId;
    private String priorityReason;
    private LocalDateTime arrivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private String reviewResult;
    private String reviewRemark;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkOrderDTO fromEntity(WorkOrder wo, String siteName) {
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.id = wo.getId();
        dto.orderNo = wo.getOrderNo();
        dto.type = wo.getType();
        dto.status = wo.getStatus();
        dto.priority = wo.getPriority();
        dto.deviceId = wo.getDeviceId();
        dto.siteId = wo.getSiteId();
        dto.siteName = siteName;
        dto.skuId = wo.getSkuId();
        dto.title = wo.getTitle();
        dto.description = wo.getDescription();
        dto.expectedQty = wo.getExpectedQty();
        dto.actualQty = wo.getActualQty();
        dto.assigneeId = wo.getAssigneeId();
        dto.priorityReason = wo.getPriorityReason();
        dto.arrivedAt = wo.getArrivedAt();
        dto.processedAt = wo.getProcessedAt();
        dto.completedAt = wo.getCompletedAt();
        dto.reviewResult = wo.getReviewResult();
        dto.reviewRemark = wo.getReviewRemark();
        dto.closedAt = wo.getClosedAt();
        dto.createdAt = wo.getCreatedAt();
        dto.updatedAt = wo.getUpdatedAt();
        return dto;
    }
}
