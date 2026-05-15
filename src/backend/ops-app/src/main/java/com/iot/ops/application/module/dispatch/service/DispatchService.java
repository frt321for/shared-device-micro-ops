package com.iot.ops.application.module.dispatch.service;

import com.iot.ops.application.module.dispatch.domain.Route;
import com.iot.ops.application.module.dispatch.domain.RouteStop;
import com.iot.ops.application.module.dispatch.repository.RouteRepository;
import com.iot.ops.application.module.dispatch.repository.RouteStopRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private SiteRepository siteRepository;

    public List<Map<String, Object>> calculatePriorities() {
        List<WorkOrder> orders = workOrderRepository.findByStatus("pending_assign");
        List<Map<String, Object>> result = new ArrayList<>();

        for (WorkOrder order : orders) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            if (order.getPriority() != null) {
                score += order.getPriority() * 10;
                reasons.add("priority=" + order.getPriority());
            }

            if (order.getCreatedAt() != null) {
                long hours = java.time.Duration.between(order.getCreatedAt(), java.time.LocalDateTime.now()).toHours();
                if (hours > 48) {
                    score += 30;
                    reasons.add("超过48小时未处理");
                } else if (hours > 24) {
                    score += 15;
                    reasons.add("超过24小时未处理");
                }
            }

            if ("urgent".equals(order.getType())) {
                score += 20;
                reasons.add("紧急工单");
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workOrderId", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("reason", String.join("; ", reasons));
            item.put("score", score);
            result.add(item);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        return result;
    }

    @Transactional
    public Route generateRoute(List<Long> workOrderIds, Long assigneeId) {
        List<WorkOrder> orders = workOrderRepository.findAllById(workOrderIds);

        List<WorkOrder> ordered = workOrderIds.stream()
                .map(id -> orders.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double totalDist = 0.0;
        int totalEst = 0;

        Route route = Route.builder()
                .name("路线-" + java.time.LocalDate.now())
                .status("pending")
                .assigneeId(assigneeId)
                .totalDistance(totalDist)
                .estimatedMinutes(totalEst)
                .build();
        route = routeRepository.save(route);

        final Long routeId = route.getId();
        List<RouteStop> stops = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            WorkOrder wo = ordered.get(i);
            Site site = wo.getSiteId() != null ? siteRepository.findById(wo.getSiteId()).orElse(null) : null;
            int estMin = 30;

            RouteStop stop = RouteStop.builder()
                    .routeId(routeId)
                    .workOrderId(wo.getId())
                    .siteId(wo.getSiteId())
                    .stopOrder(i + 1)
                    .estimatedMinutes(estMin)
                    .build();
            stops.add(stop);
        }
        routeStopRepository.saveAll(stops);

        return routeRepository.findById(routeId).orElse(route);
    }

    public Map<String, Object> getRoute(Long id) {
        Route route = routeRepository.findById(id).orElse(null);
        if (route == null) return null;

        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("route", route);
        result.put("stops", stops);
        return result;
    }

    public List<Route> getActiveRoutes() {
        return routeRepository.findByStatus("pending");
    }

    @Transactional
    public Route adjustRoute(Long routeId, List<Long> workOrderIds) {
        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) throw new RuntimeException("Route not found: " + routeId);

        routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId)
                .forEach(rs -> routeStopRepository.delete(rs));

        List<RouteStop> newStops = new ArrayList<>();
        for (int i = 0; i < workOrderIds.size(); i++) {
            RouteStop stop = RouteStop.builder()
                    .routeId(routeId)
                    .workOrderId(workOrderIds.get(i))
                    .stopOrder(i + 1)
                    .estimatedMinutes(30)
                    .build();
            newStops.add(stop);
        }
        routeStopRepository.saveAll(newStops);

        route.setAdjustmentReason("手动调整路线顺序");
        return routeRepository.save(route);
    }
}
