package com.iot.ops.application.module.dispatch.service;

import com.iot.ops.application.module.dispatch.domain.Route;
import com.iot.ops.application.module.dispatch.domain.RouteStop;
import com.iot.ops.application.module.dispatch.repository.RouteRepository;
import com.iot.ops.application.module.dispatch.repository.RouteStopRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import com.iot.ops.common.BusinessException;
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
    void calculatePriorities_shouldHandleEmptyOrders() {
        when(workOrderRepository.findByStatus("pending_assign")).thenReturn(List.of());

        List<Map<String, Object>> result = dispatchService.calculatePriorities();

        assertTrue(result.isEmpty());
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
    void getRoute_shouldReturnRouteWithStops() {
        Route route = Route.builder().id(1L).name("Route 1").status("pending").build();
        RouteStop stop = RouteStop.builder().routeId(1L).workOrderId(10L).stopOrder(1).build();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderByStopOrderAsc(1L)).thenReturn(List.of(stop));

        Map<String, Object> result = dispatchService.getRoute(1L);

        assertNotNull(result);
        assertEquals(route, result.get("route"));
        assertEquals(List.of(stop), result.get("stops"));
    }

    @Test
    void getRoute_shouldReturnNull_whenNotFound() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        Map<String, Object> result = dispatchService.getRoute(99L);

        assertNull(result);
    }

    @Test
    void getActiveRoutes_shouldReturnPendingRoutes() {
        Route route1 = Route.builder().id(1L).name("Route 1").status("pending").build();
        Route route2 = Route.builder().id(2L).name("Route 2").status("pending").build();

        when(routeRepository.findByStatus("pending")).thenReturn(List.of(route1, route2));

        List<Route> result = dispatchService.getActiveRoutes();

        assertEquals(2, result.size());
        assertEquals("Route 1", result.get(0).getName());
        assertEquals("Route 2", result.get(1).getName());
        verify(routeRepository).findByStatus("pending");
    }

    @Test
    void updateRouteStatus_shouldTransitionFromPendingToInProgress() {
        Route route = Route.builder().id(1L).name("Route 1").status("pending").build();
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Route result = dispatchService.updateRouteStatus(1L, "in_progress");

        assertEquals("in_progress", result.getStatus());
    }

    @Test
    void updateRouteStatus_shouldThrow_whenInvalidTransition() {
        Route route = Route.builder().id(1L).name("Route 1").status("pending").build();
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        assertThrows(BusinessException.class, () -> dispatchService.updateRouteStatus(1L, "completed"));
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

    @Test
    void adjustRoute_shouldThrow_whenRouteNotFound() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> dispatchService.adjustRoute(99L, List.of(1L), "reorder"));
    }
}
