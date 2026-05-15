package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final DeviceRepository deviceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final SiteRepository siteRepository;

    public Map<String, Object> getSlaOverview() {
        long totalDevices = deviceRepository.count();
        long onlineDevices = deviceRepository.countByStatus("online");
        long pendingOrders = workOrderRepository.countByStatus("pending_assign");
        long closedOrders = workOrderRepository.countByStatus("closed");
        long totalSites = siteRepository.count();

        Map<String, Object> sla = new LinkedHashMap<>();
        sla.put("totalDevices", totalDevices);
        sla.put("onlineDevices", onlineDevices);
        sla.put("deviceOnlineRate", totalDevices > 0 ? Math.round((double) onlineDevices / totalDevices * 100) + "%" : "0%");
        sla.put("pendingWorkOrders", pendingOrders);
        sla.put("completedWorkOrders", closedOrders);
        sla.put("totalSites", totalSites);
        sla.put("faultResponseTime", "N/A");
        sla.put("repairRecoveryTime", "N/A");
        sla.put("taskCompletionRate", (closedOrders + pendingOrders) > 0
            ? Math.round((double) closedOrders / (closedOrders + pendingOrders) * 100) + "%"
            : "0%");
        return sla;
    }
}
