package com.iot.ops.application.module.dispatch.service;

import com.iot.ops.application.module.dispatch.domain.Route;
import com.iot.ops.application.module.dispatch.domain.RouteStop;
import com.iot.ops.application.module.dispatch.repository.RouteRepository;
import com.iot.ops.application.module.dispatch.repository.RouteStopRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private DispatchService dispatchService;

    @Test
    void calculatePriorities_shouldReturnListSortedByScoreDesc() {
        WorkOrder high = WorkOrder.builder().id(1L).orderNo("WO001").priority(5).type("repair")
                .createdAt(LocalDateTime.now()).title("High").build();
        WorkOrder low = WorkOrder.builder().id(2L).orderNo("WO002").priority(1).type("repair")
                .createdAt(LocalDateTime.now()).title("Low").build();

        when(workOrderRepository.findByStatus("pending_assign")).thenReturn(List.of(high, low));

        List<Map<String, Object>> result = dispatchService.calculatePriorities();

        assertEquals(2, result.size());
        assertTrue((int) result.get(0).get("score") >= (int) result.get(1).get("score"));
        assertEquals(1L, result.get(0).get("workOrderId"));
    }

    @Test
    void calculatePriorities_shouldGiveBonusToUrgentOrders() {
        WorkOrder urgent = WorkOrder.builder().id(1L).orderNo("WO003").priority(2).type("urgent")
                .createdAt(LocalDateTime.now()).title("Urgent").build();

        when(workOrderRepository.findByStatus("pending_assign")).thenReturn(List.of(urgent));

        List<Map<String, Object>> result = dispatchService.calculatePriorities();

        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertTrue((int) item.get("score") >= 20);
        assertTrue(((String) item.get("reason")).contains("紧急工单"));
    }

    @Test
    void generateRoute_shouldCreateRouteWithPendingStatus() {
        WorkOrder wo = WorkOrder.builder().id(1L).orderNo("WO004").siteId(10L).build();
        Site site = Site.builder().id(10L).name("Site A").latitude(31.2).longitude(121.4).build();
        Route savedRoute = Route.builder().id(100L).name("路线-2026-01-01").status("pending").assigneeId(5L).build();
        RouteStop stop = RouteStop.builder().routeId(100L).workOrderId(1L).siteId(10L).stopOrder(1).estimatedMinutes(30).build();

        when(workOrderRepository.findAllById(List.of(1L))).thenReturn(List.of(wo));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));
        when(routeRepository.save(any(Route.class))).thenReturn(savedRoute);
        when(routeStopRepository.saveAll(anyList())).thenReturn(List.of(stop));
        when(routeRepository.findById(100L)).thenReturn(Optional.of(savedRoute));

        Route result = dispatchService.generateRoute(List.of(1L), 5L);

        assertNotNull(result);
        assertEquals("pending", result.getStatus());
        assertEquals(5L, result.getAssigneeId());
        verify(routeRepository).save(any(Route.class));
        verify(routeStopRepository).saveAll(anyList());
    }

    @Test
    void adjustRoute_shouldReorderStopsAndUpdateDistance() {
        WorkOrder wo1 = WorkOrder.builder().id(1L).siteId(10L).build();
        WorkOrder wo2 = WorkOrder.builder().id(2L).siteId(20L).build();
        Route route = Route.builder().id(100L).status("pending").totalDistance(10.0).build();
        Site site1 = Site.builder().id(10L).latitude(31.2).longitude(121.4).build();
        Site site2 = Site.builder().id(20L).latitude(31.3).longitude(121.5).build();

        when(routeRepository.findById(100L)).thenReturn(Optional.of(route));
        when(workOrderRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(wo1, wo2));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site1));
        when(siteRepository.findById(20L)).thenReturn(Optional.of(site2));
        when(routeStopRepository.findByRouteIdOrderByStopOrderAsc(100L)).thenReturn(List.of());
        when(routeStopRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Route result = dispatchService.adjustRoute(100L, List.of(2L, 1L), "reorder");

        assertEquals("reorder", result.getAdjustmentReason());
        assertNotNull(result.getTotalDistance());
        verify(routeStopRepository).findByRouteIdOrderByStopOrderAsc(100L);
        verify(routeStopRepository).saveAll(argThat((List<RouteStop> stops) ->
                stops.size() == 2 && stops.get(0).getWorkOrderId().equals(2L)));
    }
}
