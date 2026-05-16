package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
import com.iot.ops.application.module.workorder.repository.WorkOrderAuditRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderAuditRepository workOrderAuditRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private SlaService slaService;

    @Test
    void getSlaOverview_withNormalData_returnsCorrectMap() {
        when(deviceRepository.count()).thenReturn(10L);
        when(deviceRepository.countByStatus("online")).thenReturn(7L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(3L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(15L);
        when(siteRepository.count()).thenReturn(5L);
        // No closed repair orders -> response times use "N/A"
        when(workOrderRepository.findByTypeAndStatus("repair", "closed")).thenReturn(List.of());

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals(10L, sla.get("totalDevices"));
        assertEquals(7L, sla.get("onlineDevices"));
        assertEquals("70%", sla.get("deviceOnlineRate"));
        assertEquals(3L, sla.get("pendingWorkOrders"));
        assertEquals(15L, sla.get("completedWorkOrders"));
        assertEquals(5L, sla.get("totalSites"));
        assertEquals("N/A", sla.get("faultResponseTime"));
        assertEquals("N/A", sla.get("repairRecoveryTime"));
        assertEquals("83%", sla.get("taskCompletionRate"));
    }

    @Test
    void getSlaOverview_totalDevicesZero_returnsZeroRate() {
        when(deviceRepository.count()).thenReturn(0L);
        when(deviceRepository.countByStatus("online")).thenReturn(0L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(0L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(0L);
        when(siteRepository.count()).thenReturn(0L);
        when(workOrderRepository.findByTypeAndStatus("repair", "closed")).thenReturn(List.of());

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals(0L, sla.get("totalDevices"));
        assertEquals("0%", sla.get("deviceOnlineRate"));
        assertEquals("0%", sla.get("taskCompletionRate"));
        assertEquals("N/A", sla.get("faultResponseTime"));
        assertEquals("N/A", sla.get("repairRecoveryTime"));
    }

    @Test
    void getSlaOverview_noClosedRepairOrders_returnsNAResponseTime() {
        when(deviceRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus("online")).thenReturn(3L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(2L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(8L);
        when(siteRepository.count()).thenReturn(2L);
        when(workOrderRepository.findByTypeAndStatus("repair", "closed")).thenReturn(List.of());

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals("N/A", sla.get("faultResponseTime"));
        assertEquals("N/A", sla.get("repairRecoveryTime"));
    }

    @Test
    void getSlaOverview_withRepairAudits_calculatesFaultResponseTime() {
        WorkOrder repairOrder = WorkOrder.builder()
                .id(1L).type("repair").status("closed").build();

        LocalDateTime assignedTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime arrivedTime = LocalDateTime.of(2025, 1, 1, 10, 35);

        WorkOrderAudit assignedAudit = WorkOrderAudit.builder()
                .id(1L).workOrderId(1L).toStatus("assigned").createdAt(assignedTime).build();
        WorkOrderAudit arrivedAudit = WorkOrderAudit.builder()
                .id(2L).workOrderId(1L).toStatus("arrived").createdAt(arrivedTime).build();

        when(workOrderRepository.findByTypeAndStatus("repair", "closed"))
                .thenReturn(List.of(repairOrder));
        when(workOrderAuditRepository.findByWorkOrderIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(assignedAudit, arrivedAudit));

        when(deviceRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus("online")).thenReturn(4L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(0L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(1L);
        when(siteRepository.count()).thenReturn(1L);

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals("35分钟", sla.get("faultResponseTime"));
    }

    @Test
    void getSlaOverview_withRepairRecoveryTime_calculatesCorrectly() {
        LocalDateTime arrivedTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime closedTime = LocalDateTime.of(2025, 1, 1, 12, 30);

        WorkOrder repairOrder = WorkOrder.builder()
                .id(1L).type("repair").status("closed")
                .arrivedAt(arrivedTime).closedAt(closedTime).build();

        when(workOrderRepository.findByTypeAndStatus("repair", "closed"))
                .thenReturn(List.of(repairOrder));
        // No audits with matching toStatus -> faultResponseTime stays N/A
        when(workOrderAuditRepository.findByWorkOrderIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        when(deviceRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus("online")).thenReturn(3L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(1L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(4L);
        when(siteRepository.count()).thenReturn(1L);

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals("N/A", sla.get("faultResponseTime"));
        assertEquals("2小时30分钟", sla.get("repairRecoveryTime"));
    }

    @Test
    void getSlaOverview_withRepairRecoveryUnderOneHour_returnsMinutes() {
        LocalDateTime arrivedTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime closedTime = LocalDateTime.of(2025, 1, 1, 10, 45);

        WorkOrder repairOrder = WorkOrder.builder()
                .id(1L).type("repair").status("closed")
                .arrivedAt(arrivedTime).closedAt(closedTime).build();

        when(workOrderRepository.findByTypeAndStatus("repair", "closed"))
                .thenReturn(List.of(repairOrder));
        when(workOrderAuditRepository.findByWorkOrderIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        when(deviceRepository.count()).thenReturn(3L);
        when(deviceRepository.countByStatus("online")).thenReturn(2L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(0L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(2L);
        when(siteRepository.count()).thenReturn(1L);

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals("45分钟", sla.get("repairRecoveryTime"));
    }

    @Test
    void getSlaOverview_taskCompletionRateNoCompletedOrPending_returnsZero() {
        when(deviceRepository.count()).thenReturn(1L);
        when(deviceRepository.countByStatus("online")).thenReturn(1L);
        when(workOrderRepository.countByStatus("pending_assign")).thenReturn(0L);
        when(workOrderRepository.countByStatus("closed")).thenReturn(0L);
        when(siteRepository.count()).thenReturn(1L);
        when(workOrderRepository.findByTypeAndStatus("repair", "closed")).thenReturn(List.of());

        Map<String, Object> sla = slaService.getSlaOverview();

        assertEquals("0%", sla.get("taskCompletionRate"));
    }
}
