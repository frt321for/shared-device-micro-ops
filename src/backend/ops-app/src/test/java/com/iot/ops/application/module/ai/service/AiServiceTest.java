package com.iot.ops.application.module.ai.service;

import com.iot.ops.application.module.ai.domain.WeeklyReport;
import com.iot.ops.application.module.ai.repository.WeeklyReportRepository;
import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.revenue.domain.OrderEvent;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import com.iot.ops.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private DeviceStockRepository deviceStockRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private AiService aiService;

    private Device sampleDevice;

    @BeforeEach
    void setUp() {
        sampleDevice = Device.builder()
                .id(1L)
                .name("售货机-A1")
                .deviceCode("VM-001")
                .status("online")
                .build();
    }

    /* =============== generateReplenishmentNote =============== */

    @Test
    void generateReplenishmentNote_deviceNotFound_returnsErrorMessage() {
        Long deviceId = 99L;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        String result = aiService.generateReplenishmentNote(deviceId);

        assertEquals("设备 ID 99 不存在", result);
        verify(deviceRepository).findById(deviceId);
        // The service also fetches deviceStock before checking device existence
    }

    @Test
    void generateReplenishmentNote_noStock_returnsNoteWithEmptyStockSection() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(deviceStockRepository.findByDeviceId(1L)).thenReturn(List.of());

        String result = aiService.generateReplenishmentNote(1L);

        assertTrue(result.startsWith("设备 售货机-A1（VM-001）库存报告："));
        assertTrue(result.contains("库存报告"));
        verify(deviceStockRepository).findByDeviceId(1L);
    }

    @Test
    void generateReplenishmentNote_withStocks_includesStatusAndRecommendations() {
        DeviceStock adequate = DeviceStock.builder()
                .deviceId(1L).skuId(100L).quantity(50).minThreshold(10).status("adequate").build();
        DeviceStock low = DeviceStock.builder()
                .deviceId(1L).skuId(101L).quantity(5).minThreshold(10).status("low").build();
        DeviceStock soldOut = DeviceStock.builder()
                .deviceId(1L).skuId(102L).quantity(0).minThreshold(10).status("out_of_stock")
                .predictedSoldOut(LocalDateTime.now().minusDays(1)).build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(deviceStockRepository.findByDeviceId(1L)).thenReturn(List.of(adequate, low, soldOut));

        String result = aiService.generateReplenishmentNote(1L);

        assertTrue(result.contains("库存充足"));
        assertTrue(result.contains("建议优先补货"));
        assertTrue(result.contains("需紧急补货"));
        assertTrue(result.contains("预计售罄时间"));
    }

    @Test
    void generateReplenishmentNote_withPredictedSoldOut_includesTime() {
        LocalDateTime predicted = LocalDateTime.now().plusDays(3);
        DeviceStock stock = DeviceStock.builder()
                .deviceId(1L).skuId(200L).quantity(8).minThreshold(10).status("low")
                .predictedSoldOut(predicted).build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(deviceStockRepository.findByDeviceId(1L)).thenReturn(List.of(stock));

        String result = aiService.generateReplenishmentNote(1L);

        assertTrue(result.contains(predicted.toString()));
        assertTrue(result.contains("建议优先补货"));
    }

    /* =============== generateFaultAnalysis =============== */

    @Test
    void generateFaultAnalysis_deviceNotFound_returnsErrorMessage() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        String result = aiService.generateFaultAnalysis(99L);

        assertEquals("设备 ID 99 不存在", result);
    }

    @Test
    void generateFaultAnalysis_noFaults_returnsNormalStatus() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(workOrderRepository.findByDeviceId(1L)).thenReturn(List.of());

        String result = aiService.generateFaultAnalysis(1L);

        assertTrue(result.contains("近期无故障记录"));
        assertTrue(result.contains("online"));
    }

    @Test
    void generateFaultAnalysis_withFaultOrders_returnsAnalysis() {
        WorkOrder fault1 = WorkOrder.builder().id(1L).type("fault").status("closed").build();
        WorkOrder fault2 = WorkOrder.builder().id(2L).type("fault").status("processing").build();
        WorkOrder repair = WorkOrder.builder().id(3L).type("repair").status("closed").build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(workOrderRepository.findByDeviceId(1L)).thenReturn(List.of(fault1, fault2, repair));

        String result = aiService.generateFaultAnalysis(1L);

        assertTrue(result.contains("历史故障次数：3"));
        assertTrue(result.contains("故障工单状态分布"));
        assertTrue(result.contains("有 1 个故障工单尚未处理完毕"));
    }

    @Test
    void generateFaultAnalysis_allClosed_noPendingNote() {
        WorkOrder fault1 = WorkOrder.builder().id(1L).type("fault").status("closed").build();
        WorkOrder repair = WorkOrder.builder().id(2L).type("repair").status("closed").build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(workOrderRepository.findByDeviceId(1L)).thenReturn(List.of(fault1, repair));

        String result = aiService.generateFaultAnalysis(1L);

        assertTrue(result.contains("历史故障次数：2"));
        assertFalse(result.contains("尚未处理完毕"));
    }

    /* =============== generateWeeklyReport =============== */

    @Test
    void generateWeeklyReport_createsAndSavesReport() {
        Long siteId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 6);
        LocalDate end = LocalDate.of(2025, 1, 12);

        Site site = Site.builder().id(siteId).name("测试站点").build();
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

        OrderEvent event = OrderEvent.builder()
                .id(1L).siteId(siteId).amount(new BigDecimal("1500.00"))
                .eventTime(start.atStartOfDay()).build();
        when(orderEventRepository.findBySiteIdAndEventTimeBetween(eq(siteId), any(), any()))
                .thenReturn(List.of(event));

        when(workOrderRepository.findBySiteId(siteId)).thenReturn(List.of());

        WeeklyReport savedReport = WeeklyReport.builder()
                .id(1L).siteId(siteId).title("测试站点 周报（2025-01-06 ~ 2025-01-12）")
                .content("some content").periodStart(start).periodEnd(end).status("draft").build();
        when(weeklyReportRepository.save(any(WeeklyReport.class))).thenReturn(savedReport);

        WeeklyReport result = aiService.generateWeeklyReport(siteId, start, end);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("draft", result.getStatus());
        verify(weeklyReportRepository).save(any(WeeklyReport.class));
    }

    @Test
    void generateWeeklyReport_withRevenueAndFaults_includesInContent() {
        Long siteId = 2L;
        LocalDate start = LocalDate.of(2025, 2, 1);
        LocalDate end = LocalDate.of(2025, 2, 7);

        Site site = Site.builder().id(siteId).name("旗舰站点").build();
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

        OrderEvent event = OrderEvent.builder()
                .id(1L).siteId(siteId).amount(new BigDecimal("3000.00"))
                .eventTime(start.atStartOfDay()).build();
        when(orderEventRepository.findBySiteIdAndEventTimeBetween(eq(siteId), any(), any()))
                .thenReturn(List.of(event));

        WorkOrder fault = WorkOrder.builder().id(1L).type("fault").status("closed").build();
        when(workOrderRepository.findBySiteId(siteId)).thenReturn(List.of(fault));

        WeeklyReport saved = WeeklyReport.builder()
                .id(2L).siteId(siteId).title("旗舰站点 周报（2025-02-01 ~ 2025-02-07）")
                .content("with AI").periodStart(start).periodEnd(end).status("draft").build();
        when(weeklyReportRepository.save(any(WeeklyReport.class))).thenReturn(saved);

        WeeklyReport result = aiService.generateWeeklyReport(siteId, start, end);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(orderEventRepository).findBySiteIdAndEventTimeBetween(eq(siteId), any(), any());
    }

    /* =============== getWeeklyReports =============== */

    @Test
    void getWeeklyReports_returnsListOrderedByPeriodStartDesc() {
        Long siteId = 1L;
        WeeklyReport r1 = WeeklyReport.builder().id(1L).siteId(siteId).title("Report 1").build();
        WeeklyReport r2 = WeeklyReport.builder().id(2L).siteId(siteId).title("Report 2").build();

        when(weeklyReportRepository.findBySiteIdOrderByPeriodStartDesc(siteId))
                .thenReturn(List.of(r2, r1));

        List<WeeklyReport> reports = aiService.getWeeklyReports(siteId);

        assertEquals(2, reports.size());
        assertEquals("Report 2", reports.get(0).getTitle());
        verify(weeklyReportRepository).findBySiteIdOrderByPeriodStartDesc(siteId);
    }

    @Test
    void getWeeklyReports_noReports_returnsEmptyList() {
        when(weeklyReportRepository.findBySiteIdOrderByPeriodStartDesc(99L))
                .thenReturn(List.of());

        List<WeeklyReport> reports = aiService.getWeeklyReports(99L);

        assertTrue(reports.isEmpty());
    }

    /* =============== getWeeklyReport =============== */

    @Test
    void getWeeklyReport_found_returnsReport() {
        WeeklyReport report = WeeklyReport.builder().id(1L).siteId(1L).title("Found Report").build();
        when(weeklyReportRepository.findById(1L)).thenReturn(Optional.of(report));

        WeeklyReport result = aiService.getWeeklyReport(1L);

        assertEquals("Found Report", result.getTitle());
    }

    @Test
    void getWeeklyReport_notFound_throwsBusinessException() {
        when(weeklyReportRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiService.getWeeklyReport(99L));
        assertTrue(ex.getMessage().contains("Report not found"));
    }

    /* =============== AI API path via ReflectionTestUtils =============== */

    @SuppressWarnings("unchecked")
    @Test
    void generateReplenishmentNote_withAiApiKey_usesAiResult() {
        // Mock RestTemplate so AI API path can succeed
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(aiService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(aiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "aiBaseUrl", "https://api.test.com");
        ReflectionTestUtils.setField(aiService, "aiModel", "test-model");

        DeviceStock stock = DeviceStock.builder()
                .deviceId(1L).skuId(100L).quantity(20).minThreshold(10).status("adequate").build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(deviceStockRepository.findByDeviceId(1L)).thenReturn(List.of(stock));

        ResponseEntity<Map> mockResponse = mock(ResponseEntity.class);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        Map<String, Object> choice = new LinkedHashMap<>();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("content", "AI 生成的补货建议：建议补货 SKU 100...");
        choice.put("message", message);
        responseBody.put("choices", List.of(choice));

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getBody()).thenReturn(responseBody);

        when(mockRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(mockResponse);

        String result = aiService.generateReplenishmentNote(1L);

        assertTrue(result.contains("AI 生成的补货建议"));
        verify(mockRestTemplate).exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }
}
