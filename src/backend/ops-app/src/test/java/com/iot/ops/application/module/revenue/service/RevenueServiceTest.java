package com.iot.ops.application.module.revenue.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import com.iot.ops.application.module.revenue.domain.OrderEvent;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.repository.SiteRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
