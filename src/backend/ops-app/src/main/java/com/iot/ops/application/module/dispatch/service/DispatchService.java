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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private SiteRepository siteRepository;

    private double haversineDistance(Site s1, Site s2) {
        if (s1 == null || s2 == null
                || s1.getLatitude() == null || s1.getLongitude() == null
                || s2.getLatitude() == null || s2.getLongitude() == null) {
            return 0.0;
        }
        double dLat = Math.toRadians(s2.getLatitude() - s1.getLatitude());
        double dLon = Math.toRadians(s2.getLongitude() - s1.getLongitude());
        double lat1 = Math.toRadians(s1.getLatitude());
        double lat2 = Math.toRadians(s2.getLatitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private List<WorkOrder> nearestNeighborOptimize(List<WorkOrder> orders, Map<Long, Site> siteMap) {
        if (orders.size() <= 2) return orders;
        List<WorkOrder> remaining = new ArrayList<>(orders);
        List<WorkOrder> optimized = new ArrayList<>();
        WorkOrder current = remaining.remove(0);
        optimized.add(current);
        while (!remaining.isEmpty()) {
            Site currentSite = siteMap.get(current.getSiteId());
            int nearestIdx = 0;
            double nearestDist = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                Site candidate = siteMap.get(remaining.get(i).getSiteId());
                double dist = haversineDistance(currentSite, candidate);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }
            current = remaining.remove(nearestIdx);
            optimized.add(current);
        }
        return optimized;
    }

    private double calculateRouteDistance(List<WorkOrder> orders, Map<Long, Site> siteMap) {
        double total = 0.0;
        for (int i = 0; i < orders.size() - 1; i++) {
            Site s1 = siteMap.get(orders.get(i).getSiteId());
            Site s2 = siteMap.get(orders.get(i + 1).getSiteId());
            total += haversineDistance(s1, s2);
        }
        return total;
    }

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

        Map<Long, Site> siteMap = new HashMap<>();
        for (WorkOrder wo : ordered) {
            if (wo.getSiteId() != null && !siteMap.containsKey(wo.getSiteId())) {
                siteRepository.findById(wo.getSiteId()).ifPresent(s -> siteMap.put(wo.getSiteId(), s));
            }
        }

        ordered = nearestNeighborOptimize(ordered, siteMap);
        double totalDist = calculateRouteDistance(ordered, siteMap);

        int totalEst = ordered.size() * 30;

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
    public Route updateRouteStatus(Long routeId, String status) {
        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) throw new BusinessException("Route not found: " + routeId);

        String current = route.getStatus();
        if ("pending".equals(current) && "in_progress".equals(status)) {
            route.setStatus("in_progress");
        } else if ("in_progress".equals(current) && "completed".equals(status)) {
            route.setStatus("completed");
        } else {
            route.setStatus(status);
        }
        return routeRepository.save(route);
    }

    @Transactional
    public Route adjustRoute(Long routeId, List<Long> workOrderIds, String reason) {
        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) throw new BusinessException("Route not found: " + routeId);

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

        route.setAdjustmentReason(reason);
        return routeRepository.save(route);
    }
}
