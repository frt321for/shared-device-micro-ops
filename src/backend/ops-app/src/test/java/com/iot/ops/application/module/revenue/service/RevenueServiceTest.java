package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.revenue.domain.OrderEvent;
import com.iot.ops.application.module.revenue.domain.SiteRevenue;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void getOverview_shouldContainAllExpectedKeys() {
        OrderEvent event = OrderEvent.builder()
                .amount(new BigDecimal("100"))
                .eventTime(LocalDateTime.now())
                .build();

        when(orderEventRepository.sumAmount()).thenReturn(new BigDecimal("100"));
        when(orderEventRepository.count()).thenReturn(1L);
        when(siteRepository.count()).thenReturn(3L);
        when(deviceRepository.countByStatus("online")).thenReturn(10L);
        when(deviceRepository.count()).thenReturn(15L);

        Map<String, Object> overview = revenueService.getOverview();

        assertNotNull(overview);
        assertEquals(new BigDecimal("100"), overview.get("totalRevenue"));
        assertEquals(1L, overview.get("totalOrders"));
        assertEquals(3L, overview.get("totalSites"));
        assertEquals(15L, overview.get("totalDevices"));
        assertEquals(10L, overview.get("activeDevices"));
    }

    @Test
    void getOverview_shouldReturnZeroValuesWhenNoEvents() {
        when(orderEventRepository.sumAmount()).thenReturn(BigDecimal.ZERO);
        when(orderEventRepository.count()).thenReturn(0L);
        when(siteRepository.count()).thenReturn(0L);
        when(deviceRepository.countByStatus("online")).thenReturn(0L);
        when(deviceRepository.count()).thenReturn(0L);

        Map<String, Object> overview = revenueService.getOverview();

        assertEquals(BigDecimal.ZERO, overview.get("totalRevenue"));
        assertEquals(0L, overview.get("totalOrders"));
    }

    @Test
    void getSiteRankings_withEmptyData_shouldReturnEmptyList() {
        var rankings = revenueService.getSiteRankings(null, null);

        assertTrue(rankings.isEmpty());
    }

    @Test
    void getSiteRankings_shouldReturnSortedByRevenueDesc() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        OrderEvent e1 = OrderEvent.builder()
                .siteId(1L).amount(new BigDecimal("50")).eventTime(now).build();
        OrderEvent e2 = OrderEvent.builder()
                .siteId(2L).amount(new BigDecimal("200")).eventTime(now).build();

        when(orderEventRepository.findByEventTimeBetween(any(), any())).thenReturn(List.of(e1, e2));

        var rankings = revenueService.getSiteRankings(today.minusDays(1), today.plusDays(1));

        assertEquals(2, rankings.size());
        assertTrue(rankings.get(0).getTotalRevenue().compareTo(rankings.get(1).getTotalRevenue()) >= 0);
    }

    @Test
    void getSiteRankings_shouldHandleEmptyEvents() {
        LocalDate today = LocalDate.now();

        when(orderEventRepository.findByEventTimeBetween(any(), any())).thenReturn(List.of());
        lenient().when(siteRepository.findAllById(any())).thenReturn(List.of());
        lenient().when(skuRepository.findAllById(any())).thenReturn(List.of());

        List<SiteRevenue> result = revenueService.getSiteRankings(today.minusDays(1), today.plusDays(1));

        assertTrue(result.isEmpty());
    }

    @Test
    void getSiteDetail_shouldHandleDefaultDates() {
        when(orderEventRepository.findBySiteIdAndEventTimeBetween(eq(1L), any(), any())).thenReturn(List.of());
        when(siteRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(skuRepository.findAllById(any())).thenReturn(List.of());

        SiteRevenue result = revenueService.getSiteDetail(1L, null, null);

        assertNotNull(result);
        assertEquals(1L, result.getSiteId());
        assertEquals("Unknown", result.getSiteName());
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        assertEquals(0L, result.getOrderCount());
    }

    @Test
    void getDeviceEfficiency_shouldCalculateEfficiency() {
        LocalDateTime now = LocalDateTime.now();
        OrderEvent e1 = OrderEvent.builder().deviceId(10L).amount(new BigDecimal("100")).eventTime(now).build();

        when(orderEventRepository.findByEventTimeBetween(any(), any())).thenReturn(List.of(e1));
        Device device = Device.builder().id(10L).deviceCode("D001").name("Device 1").build();
        when(deviceRepository.findAll()).thenReturn(List.of(device));

        List<Map<String, Object>> result = revenueService.getDeviceEfficiency();

        assertEquals(1, result.size());
        Map<String, Object> entry = result.get(0);
        assertEquals(10L, entry.get("deviceId"));
        assertEquals("D001", entry.get("deviceCode"));
        assertEquals("Device 1", entry.get("deviceName"));
        assertEquals(1, entry.get("orderCount"));
        assertEquals(new BigDecimal("100"), entry.get("revenue"));
        assertEquals(2, entry.get("efficiency"));
    }

    @Test
    void getSkuAnalysis_shouldReturnSortedByRevenue() {
        LocalDateTime now = LocalDateTime.now();
        OrderEvent e1 = OrderEvent.builder().skuId(1L).amount(new BigDecimal("50")).eventTime(now).build();
        OrderEvent e2 = OrderEvent.builder().skuId(2L).amount(new BigDecimal("200")).eventTime(now).build();

        when(orderEventRepository.findByEventTimeBetween(any(), any())).thenReturn(List.of(e1, e2));
        when(skuRepository.findAllById(any())).thenReturn(List.of(
                Sku.builder().id(1L).name("SKU A").code("A001").build(),
                Sku.builder().id(2L).name("SKU B").code("B001").build()
        ));

        List<Map<String, Object>> result = revenueService.getSkuAnalysis();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).get("skuId"));
        assertEquals(new BigDecimal("200"), result.get(0).get("totalRevenue"));
        assertEquals(1L, result.get(1).get("skuId"));
        assertEquals(new BigDecimal("50"), result.get(1).get("totalRevenue"));
    }

    @Test
    void getSkuAnalysis_shouldHandleEmptyEvents() {
        when(orderEventRepository.findByEventTimeBetween(any(), any())).thenReturn(List.of());

        List<Map<String, Object>> result = revenueService.getSkuAnalysis();

        assertTrue(result.isEmpty());
    }
}
