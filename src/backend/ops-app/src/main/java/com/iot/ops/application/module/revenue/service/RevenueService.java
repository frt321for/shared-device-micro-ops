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

        Map<Long, Device> deviceMap = deviceRepository.findAll().stream()
                .collect(Collectors.toMap(Device::getId, d -> d));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<OrderEvent>> entry : byDevice.entrySet()) {
            Long deviceId = entry.getKey();
            List<OrderEvent> events = entry.getValue();
            Device device = deviceMap.get(deviceId);

            BigDecimal totalAmount = events.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(OrderEvent::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", deviceId);
            item.put("deviceName", device != null ? device.getName() : "Unknown");
            item.put("totalOrders", (long) events.size());
            item.put("totalRevenue", totalAmount);
            result.add(item);
        }

        result.sort((a, b) -> ((Long) b.get("totalOrders")).compareTo((Long) a.get("totalOrders")));
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

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalRevenue", totalRevenue);
        overview.put("totalOrders", totalOrders);
        overview.put("totalSites", totalSites);
        return overview;
    }
}
