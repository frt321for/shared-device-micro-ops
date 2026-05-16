package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
import com.iot.ops.application.module.workorder.repository.WorkOrderAuditRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final DeviceRepository deviceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderAuditRepository workOrderAuditRepository;
    private final OrderEventRepository orderEventRepository;
    private final SiteRepository siteRepository;

    public Map<String, Object> getSlaOverview() {
        long totalDevices = deviceRepository.count();
        long onlineDevices = deviceRepository.countByStatus("online");
        long pendingOrders = workOrderRepository.countByStatus("pending_assign");
        long closedOrders = workOrderRepository.countByStatus("closed");
        long totalSites = siteRepository.count();

        String faultResponseTime = calculateFaultResponseTime();
        String repairRecoveryTime = calculateRepairRecoveryTime();

        Map<String, Object> sla = new LinkedHashMap<>();
        sla.put("totalDevices", totalDevices);
        sla.put("onlineDevices", onlineDevices);
        sla.put("deviceOnlineRate", totalDevices > 0 ? Math.round((double) onlineDevices / totalDevices * 100) + "%" : "0%");
        sla.put("pendingWorkOrders", pendingOrders);
        sla.put("completedWorkOrders", closedOrders);
        sla.put("totalSites", totalSites);
        sla.put("faultResponseTime", faultResponseTime);
        sla.put("repairRecoveryTime", repairRecoveryTime);
        sla.put("taskCompletionRate", (closedOrders + pendingOrders) > 0
            ? Math.round((double) closedOrders / (closedOrders + pendingOrders) * 100) + "%"
            : "0%");
        return sla;
    }

    private String calculateFaultResponseTime() {
        List<WorkOrder> repairOrders = workOrderRepository.findByTypeAndStatus("repair", "closed");
        if (repairOrders.isEmpty()) return "N/A";

        long totalMinutes = 0;
        int count = 0;
        for (WorkOrder wo : repairOrders) {
            List<WorkOrderAudit> audits = workOrderAuditRepository.findByWorkOrderIdOrderByCreatedAtAsc(wo.getId());
            LocalDateTime assignedTime = null;
            LocalDateTime arrivedTime = null;
            for (WorkOrderAudit a : audits) {
                if ("assigned".equals(a.getToStatus())) assignedTime = a.getCreatedAt();
                if ("arrived".equals(a.getToStatus())) arrivedTime = a.getCreatedAt();
            }
            if (assignedTime != null && arrivedTime != null) {
                totalMinutes += Duration.between(assignedTime, arrivedTime).toMinutes();
                count++;
            }
        }
        if (count == 0) return "N/A";
        long avg = totalMinutes / count;
        return avg < 60 ? avg + "分钟" : (avg / 60) + "小时" + (avg % 60) + "分钟";
    }

    private String calculateRepairRecoveryTime() {
        List<WorkOrder> repairOrders = workOrderRepository.findByTypeAndStatus("repair", "closed");
        if (repairOrders.isEmpty()) return "N/A";

        long totalMinutes = 0;
        int count = 0;
        for (WorkOrder wo : repairOrders) {
            if (wo.getArrivedAt() != null && wo.getClosedAt() != null) {
                totalMinutes += Duration.between(wo.getArrivedAt(), wo.getClosedAt()).toMinutes();
                count++;
            }
        }
        if (count == 0) return "N/A";
        long avg = totalMinutes / count;
        return avg < 60 ? avg + "分钟" : (avg / 60) + "小时" + (avg % 60) + "分钟";
    }
}
