package com.iot.ops.application.module.dashboard.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private DeviceStockRepository deviceStockRepository;

    public Map<String, Object> getOverview() {
        List<Site> allSites = siteRepository.findAll();
        List<Device> allDevices = deviceRepository.findAll();
        List<WorkOrder> pendingOrders = workOrderRepository.findByStatus("pending_assign");
        List<DeviceStock> lowStockItems = deviceStockRepository.findByQuantityLessThan(10);

        long onlineCount = allDevices.stream().filter(d -> "online".equals(d.getStatus())).count();
        long faultCount = allDevices.stream().filter(d -> "fault".equals(d.getStatus())).count();
        long activeRoutes = countActiveRoutes();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalSites", allSites.size());
        overview.put("totalDevices", allDevices.size());
        overview.put("onlineDevices", onlineCount);
        overview.put("faultDevices", faultCount);
        overview.put("lowStockCount", lowStockItems.size());
        overview.put("pendingWorkOrders", pendingOrders.size());
        overview.put("activeRoutes", activeRoutes);
        return overview;
    }

    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Device> faultDevices = deviceRepository.findByStatus("fault");
        for (Device d : faultDevices) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("type", "device_fault");
            alert.put("level", "error");
            alert.put("message", "设备 " + d.getName() + " 故障");
            alert.put("deviceId", d.getId());
            alert.put("time", d.getUpdatedAt());
            alerts.add(alert);
        }

        List<DeviceStock> lowStock = deviceStockRepository.findByQuantityLessThan(10);
        for (DeviceStock ds : lowStock) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("type", "low_stock");
            alert.put("level", "warn");
            alert.put("message", "设备库存不足 deviceId=" + ds.getDeviceId() + ", skuId=" + ds.getSkuId() + ", 当前=" + ds.getQuantity());
            alert.put("deviceId", ds.getDeviceId());
            alert.put("time", ds.getUpdatedAt());
            alerts.add(alert);
        }

        alerts.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("time");
            LocalDateTime tb = (LocalDateTime) b.get("time");
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        return alerts;
    }

    public Map<String, Object> getDeviceStats() {
        List<Object[]> rows = deviceRepository.countByStatusGroup();

        Map<String, Object> stats = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : rows) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            stats.put(status, count);
            total += count;
        }
        stats.putIfAbsent("online", 0L);
        stats.putIfAbsent("offline", 0L);
        stats.putIfAbsent("fault", 0L);
        stats.putIfAbsent("maintenance", 0L);
        stats.put("total", total);
        return stats;
    }

    public List<WorkOrder> getTodayTasks() {
        return workOrderRepository.findByStatusNotIn(
                List.of("closed", "cancelled"),
                Pageable.ofSize(20)
        );
    }

    private long countActiveRoutes() {
        return 0;
    }
}
