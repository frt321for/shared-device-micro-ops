package com.iot.ops.application.module.dashboard.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.dispatch.domain.Route;
import com.iot.ops.application.module.dispatch.repository.RouteRepository;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private DeviceStockRepository deviceStockRepository;

    @Mock
    private RouteRepository routeRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // DashboardService uses explicit constructor with @Value int parameter
        dashboardService = new DashboardService(
                siteRepository, deviceRepository, workOrderRepository,
                deviceStockRepository, routeRepository, 10);
    }

    /* =============== getOverview =============== */

    @Test
    void getOverview_returnsCorrectMap() {
        when(siteRepository.findAll()).thenReturn(List.of(
                Site.builder().id(1L).name("Site A").build(),
                Site.builder().id(2L).name("Site B").build()));

        when(deviceRepository.findAll()).thenReturn(List.of(
                Device.builder().id(1L).status("online").build(),
                Device.builder().id(2L).status("online").build(),
                Device.builder().id(3L).status("fault").build(),
                Device.builder().id(4L).status("offline").build()));

        when(workOrderRepository.findByStatus("pending_assign")).thenReturn(
                List.of(WorkOrder.builder().id(1L).status("pending_assign").build()));

        when(deviceStockRepository.findByQuantityLessThan(10)).thenReturn(
                List.of(DeviceStock.builder().id(1L).quantity(5).build()));

        when(routeRepository.findByStatus("assigned")).thenReturn(
                List.of(Route.builder().id(1L).status("assigned").build()));
        when(routeRepository.findByStatus("in_progress")).thenReturn(
                List.of(Route.builder().id(2L).status("in_progress").build(),
                        Route.builder().id(3L).status("in_progress").build()));

        Map<String, Object> overview = dashboardService.getOverview();

        assertEquals(2, overview.get("totalSites"));
        assertEquals(4, overview.get("totalDevices"));
        assertEquals(2L, overview.get("onlineDevices"));
        assertEquals(1L, overview.get("faultDevices"));
        assertEquals(1, overview.get("lowStockCount"));
        assertEquals(1, overview.get("pendingWorkOrders"));
        assertEquals(3L, overview.get("activeRoutes"));
    }

    @Test
    void getOverview_noData_returnsZeroValues() {
        when(siteRepository.findAll()).thenReturn(List.of());
        when(deviceRepository.findAll()).thenReturn(List.of());
        when(workOrderRepository.findByStatus("pending_assign")).thenReturn(List.of());
        when(deviceStockRepository.findByQuantityLessThan(10)).thenReturn(List.of());
        when(routeRepository.findByStatus("assigned")).thenReturn(List.of());
        when(routeRepository.findByStatus("in_progress")).thenReturn(List.of());

        Map<String, Object> overview = dashboardService.getOverview();

        assertEquals(0, overview.get("totalSites"));
        assertEquals(0, overview.get("totalDevices"));
        assertEquals(0L, overview.get("onlineDevices"));
        assertEquals(0L, overview.get("faultDevices"));
        assertEquals(0, overview.get("lowStockCount"));
        assertEquals(0, overview.get("pendingWorkOrders"));
        assertEquals(0L, overview.get("activeRoutes"));
    }

    /* =============== getAlerts =============== */

    @Test
    void getAlerts_returnsSortedAlerts() {
        LocalDateTime now = LocalDateTime.now();
        Device faultDevice = Device.builder()
                .id(1L).name("Faulty VM").status("fault")
                .updatedAt(now.minusHours(2)).build();
        when(deviceRepository.findByStatus("fault")).thenReturn(List.of(faultDevice));

        DeviceStock lowStock = DeviceStock.builder()
                .id(1L).deviceId(2L).skuId(100L).quantity(3)
                .updatedAt(now.minusHours(1)).build();
        when(deviceStockRepository.findByQuantityLessThan(10)).thenReturn(List.of(lowStock));

        List<Map<String, Object>> alerts = dashboardService.getAlerts();

        assertEquals(2, alerts.size());
        // Sorted by time descending: newer alert first
        assertEquals("low_stock", alerts.get(0).get("type"));
        assertEquals("device_fault", alerts.get(1).get("type"));
        assertEquals("error", alerts.get(1).get("level"));
        assertEquals("warn", alerts.get(0).get("level"));
    }

    @Test
    void getAlerts_noFaultsOrLowStock_returnsEmptyList() {
        when(deviceRepository.findByStatus("fault")).thenReturn(List.of());
        when(deviceStockRepository.findByQuantityLessThan(10)).thenReturn(List.of());

        List<Map<String, Object>> alerts = dashboardService.getAlerts();

        assertTrue(alerts.isEmpty());
    }

    @Test
    void getAlerts_faultWithoutUpdatedAt_isSortedLast() {
        Device faultDevice = Device.builder()
                .id(1L).name("Faulty").status("fault")
                .updatedAt(null).build();
        when(deviceRepository.findByStatus("fault")).thenReturn(List.of(faultDevice));
        when(deviceStockRepository.findByQuantityLessThan(10)).thenReturn(List.of());

        List<Map<String, Object>> alerts = dashboardService.getAlerts();

        assertEquals(1, alerts.size());
        assertEquals("device_fault", alerts.get(0).get("type"));
    }

    /* =============== getDeviceStats =============== */

    @Test
    void getDeviceStats_returnsStatsFromQuery() {
        when(deviceRepository.countByStatusGroup()).thenReturn(List.of(
                new Object[]{"online", 10L},
                new Object[]{"offline", 3L},
                new Object[]{"fault", 2L}
        ));

        Map<String, Object> stats = dashboardService.getDeviceStats();

        assertEquals(10L, stats.get("online"));
        assertEquals(3L, stats.get("offline"));
        assertEquals(2L, stats.get("fault"));
        assertEquals(0L, stats.get("maintenance"));
        assertEquals(15L, stats.get("total"));
    }

    @Test
    void getDeviceStats_missingStatus_usesDefaults() {
        when(deviceRepository.countByStatusGroup()).thenReturn(List.<Object[]>of(
                new Object[]{"online", 5L}
        ));

        Map<String, Object> stats = dashboardService.getDeviceStats();

        assertEquals(5L, stats.get("online"));
        assertEquals(0L, stats.get("offline"));
        assertEquals(0L, stats.get("fault"));
        assertEquals(0L, stats.get("maintenance"));
        assertEquals(5L, stats.get("total"));
    }

    /* =============== getTodayTasks =============== */

    @Test
    void getTodayTasks_returnsOpenTasks() {
        WorkOrder task1 = WorkOrder.builder().id(1L).title("Fix pump").status("pending_assign").build();
        WorkOrder task2 = WorkOrder.builder().id(2L).title("Restock VM-5").status("processing").build();

        when(workOrderRepository.findByStatusNotIn(
                eq(List.of("closed", "cancelled")), any(Pageable.class)))
                .thenReturn(List.of(task1, task2));

        List<WorkOrder> tasks = dashboardService.getTodayTasks();

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> "Fix pump".equals(t.getTitle())));
        verify(workOrderRepository).findByStatusNotIn(
                eq(List.of("closed", "cancelled")), any(Pageable.class));
    }

    @Test
    void getTodayTasks_allClosed_returnsEmptyList() {
        when(workOrderRepository.findByStatusNotIn(
                eq(List.of("closed", "cancelled")), any(Pageable.class)))
                .thenReturn(List.of());

        List<WorkOrder> tasks = dashboardService.getTodayTasks();

        assertTrue(tasks.isEmpty());
    }
}
