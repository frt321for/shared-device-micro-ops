package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.revenue.domain.OrderEvent;
import com.iot.ops.application.module.revenue.domain.SiteRevenue;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final OrderEventRepository orderEventRepository;
    private final SiteRepository siteRepository;
    private final DeviceRepository deviceRepository;

    public List<SiteRevenue> getSiteRankings(LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        List<OrderEvent> events = orderEventRepository.findAll().stream()
                .filter(e -> e.getEventTime() != null
                        && !e.getEventTime().isBefore(startTime)
                        && !e.getEventTime().isAfter(endTime))
                .collect(Collectors.toList());

        Map<Long, List<OrderEvent>> grouped = events.stream()
                .filter(e -> e.getSiteId() != null)
                .collect(Collectors.groupingBy(OrderEvent::getSiteId));

        Map<Long, Site> siteMap = siteRepository.findAll().stream()
                .collect(Collectors.toMap(Site::getId, s -> s));

        List<SiteRevenue> result = new ArrayList<>();
        for (Map.Entry<Long, List<OrderEvent>> entry : grouped.entrySet()) {
            Long siteId = entry.getKey();
            List<OrderEvent> siteEvents = entry.getValue();
            Site site = siteMap.get(siteId);

            BigDecimal totalRevenue = siteEvents.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(OrderEvent::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            SiteRevenue sr = SiteRevenue.builder()
                    .siteId(siteId)
                    .siteName(site != null ? site.getName() : "Unknown")
                    .totalRevenue(totalRevenue)
                    .orderCount((long) siteEvents.size())
                    .totalOrders((long) siteEvents.size())
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .totalCost(BigDecimal.ZERO)
                    .grossProfit(BigDecimal.ZERO)
                    .lossAmount(BigDecimal.ZERO)
                    .build();
            result.add(sr);
        }

        result.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        return result;
    }

    public SiteRevenue getSiteDetail(Long siteId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        List<OrderEvent> events = orderEventRepository.findBySiteIdAndEventTimeBetween(siteId, startTime, endTime);

        Optional<Site> siteOpt = siteRepository.findById(siteId);

        BigDecimal totalRevenue = events.stream()
                .filter(e -> e.getAmount() != null)
                .map(OrderEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SiteRevenue.builder()
                .siteId(siteId)
                .siteName(siteOpt.map(Site::getName).orElse("Unknown"))
                .totalRevenue(totalRevenue)
                .orderCount((long) events.size())
                .totalOrders((long) events.size())
                .periodStart(startTime)
                .periodEnd(endTime)
                .totalCost(BigDecimal.ZERO)
                .grossProfit(BigDecimal.ZERO)
                .lossAmount(BigDecimal.ZERO)
                .build();
    }

    public List<Map<String, Object>> getDeviceEfficiency() {
        List<OrderEvent> allEvents = orderEventRepository.findAll();
        Map<Long, List<OrderEvent>> byDevice = allEvents.stream()
                .filter(e -> e.getDeviceId() != null)
                .collect(Collectors.groupingBy(OrderEvent::getDeviceId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Device device : deviceRepository.findAll()) {
            Long deviceId = device.getId();
            List<OrderEvent> events = byDevice.getOrDefault(deviceId, List.of());

            BigDecimal totalAmount = events.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(OrderEvent::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int orderCount = events.size();

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("deviceId", deviceId);
            entry.put("deviceCode", device.getDeviceCode());
            entry.put("deviceName", device.getName());
            entry.put("orderCount", orderCount);
            entry.put("revenue", totalAmount);
            entry.put("efficiency", orderCount > 10 ? Math.min(100, orderCount * 5) : 0);
            result.add(entry);
        }
        return result;
    }

    public Map<String, Object> getOverview() {
        List<OrderEvent> allEvents = orderEventRepository.findAll();
        BigDecimal totalRevenue = allEvents.stream()
                .filter(e -> e.getAmount() != null)
                .map(OrderEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = allEvents.size();
        long totalSites = siteRepository.count();
        long activeDevices = deviceRepository.countByStatus("online");
        long totalDevices = deviceRepository.count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalRevenue", totalRevenue);
        overview.put("totalOrders", totalOrders);
        overview.put("totalSites", totalSites);
        overview.put("totalDevices", totalDevices);
        overview.put("activeDevices", activeDevices);
        return overview;
    }
}
