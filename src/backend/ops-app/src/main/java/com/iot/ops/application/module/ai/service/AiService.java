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
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final DeviceStockRepository deviceStockRepository;
    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final OrderEventRepository orderEventRepository;
    private final WorkOrderRepository workOrderRepository;

    private static final String API_URL = "https://api-inference.modelscope.cn/v1/chat/completions";

    public String generateReplenishmentNote(Long deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        List<DeviceStock> stocks = deviceStockRepository.findByDeviceId(deviceId);

        if (deviceOpt.isEmpty()) {
            return "设备 ID " + deviceId + " 不存在";
        }

        Device device = deviceOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("设备 ").append(device.getName()).append("（").append(device.getDeviceCode()).append("）库存报告：\n");

        for (DeviceStock stock : stocks) {
            int remain = stock.getQuantity();
            int threshold = stock.getMinThreshold();
            String status = remain <= 0 ? "已售罄"
                    : remain <= threshold ? "库存不足"
                    : "库存充足";

            sb.append("  · SKU ").append(stock.getSkuId())
                    .append("：当前库存 ").append(remain).append(" 件")
                    .append("（阈值 ").append(threshold).append(" 件）")
                    .append("，状态：").append(status);

            if (remain > 0 && remain <= threshold) {
                sb.append("，建议优先补货");
            } else if (remain == 0) {
                sb.append("，需紧急补货");
            }

            if (stock.getPredictedSoldOut() != null) {
                sb.append("，预计售罄时间：").append(stock.getPredictedSoldOut().toString());
            }
            sb.append("\n");
        }

        String template = sb.toString();
        String aiResult = callAiApi("补货建议", template);
        return aiResult != null ? aiResult : template;
    }

    public String generateFaultAnalysis(Long deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return "设备 ID " + deviceId + " 不存在";
        }

        Device device = deviceOpt.get();
        List<WorkOrder> faultOrders = workOrderRepository.findByDeviceId(deviceId).stream()
                .filter(wo -> "fault".equals(wo.getType()) || "repair".equals(wo.getType()))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("设备 ").append(device.getName()).append("（").append(device.getDeviceCode()).append("）故障分析：\n\n");

        if (faultOrders.isEmpty()) {
            sb.append("该设备近期无故障记录，运行状态正常。\n");
            sb.append("当前状态：").append(device.getStatus()).append("\n");
            return sb.toString();
        }

        sb.append("历史故障次数：").append(faultOrders.size()).append(" 次\n\n");

        Map<String, Long> statusCount = faultOrders.stream()
                .collect(Collectors.groupingBy(wo -> wo.getStatus() != null ? wo.getStatus() : "unknown", Collectors.counting()));
        sb.append("故障工单状态分布：\n");
        statusCount.forEach((status, count) ->
                sb.append("  · ").append(status).append("：").append(count).append(" 条\n"));

        sb.append("\n建议：\n");
        long pendingCount = faultOrders.stream()
                .filter(wo -> "pending_assign".equals(wo.getStatus()) || "in_progress".equals(wo.getStatus()))
                .count();
        if (pendingCount > 0) {
            sb.append("  · 有 ").append(pendingCount).append(" 个故障工单尚未处理完毕，请尽快安排维修\n");
        }
        sb.append("  · 当前设备状态：").append(device.getStatus()).append("\n");

        String template = sb.toString();
        String aiResult = callAiApi("故障分析", template);
        return aiResult != null ? aiResult : template;
    }

    public WeeklyReport generateWeeklyReport(Long siteId, LocalDate periodStart, LocalDate periodEnd) {
        Optional<Site> siteOpt = siteRepository.findById(siteId);
        String siteName = siteOpt.map(Site::getName).orElse("Unknown");

        LocalDateTime startTime = periodStart.atStartOfDay();
        LocalDateTime endTime = periodEnd.atTime(LocalTime.MAX);

        List<OrderEvent> events = orderEventRepository.findBySiteIdAndEventTimeBetween(siteId, startTime, endTime);
        List<WorkOrder> workOrders = workOrderRepository.findBySiteId(siteId);

        BigDecimal totalRevenue = events.stream()
                .filter(e -> e.getAmount() != null)
                .map(OrderEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = events.size();
        long faultCount = workOrders.stream()
                .filter(wo -> "fault".equals(wo.getType()))
                .count();

        String content = buildWeeklyReportContent(siteName, totalRevenue, totalOrders,
                faultCount, workOrders.size(), periodStart, periodEnd);

        String title = siteName + " 周报（" + periodStart + " ~ " + periodEnd + "）";

        String aiContent = callAiApi(title, content);

        WeeklyReport report = WeeklyReport.builder()
                .siteId(siteId)
                .title(title)
                .content(aiContent != null ? aiContent : content)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status("draft")
                .build();

        return weeklyReportRepository.save(report);
    }

    public List<WeeklyReport> getWeeklyReports(Long siteId) {
        return weeklyReportRepository.findBySiteIdOrderByPeriodStartDesc(siteId);
    }

    public WeeklyReport getWeeklyReport(Long id) {
        return weeklyReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Report not found: " + id));
    }

    private String buildWeeklyReportContent(String siteName, BigDecimal totalRevenue,
                                             long totalOrders, long faultCount,
                                             long totalWorkOrders, LocalDate start, LocalDate end) {
        StringBuilder sb = new StringBuilder();
        sb.append("站点：").append(siteName).append("\n");
        sb.append("周期：").append(start).append(" ~ ").append(end).append("\n\n");
        sb.append("【收入概况】\n");
        sb.append("  总营收：¥").append(totalRevenue).append("\n");
        sb.append("  总订单数：").append(totalOrders).append("\n\n");
        sb.append("【设备与工单】\n");
        sb.append("  故障次数：").append(faultCount).append("\n");
        sb.append("  工单总数：").append(totalWorkOrders).append("\n\n");
        sb.append("【库存建议】\n");
        sb.append("  请检查各设备库存水平，及时补货。\n\n");
        sb.append("【总结】\n");
        sb.append("  本周站点整体运营情况良好");
        if (faultCount > 0) {
            sb.append("，存在 ").append(faultCount).append(" 次故障需关注");
        }
        if (totalOrders == 0) {
            sb.append("，但无订单产生，建议检查设备运行状态");
        }
        sb.append("。\n");
        return sb.toString();
    }

    private String callAiApi(String title, String fallbackContent) {
        String apiKey = System.getenv("MS_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("modelscope.api.key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", "请基于以下数据生成一份周报内容总结：" + fallbackContent);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "Qwen/Qwen2.5-7B-Instruct");
            requestBody.put("messages", List.of(message));
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> msg = (Map<String, Object>) choice.get("message");
                    if (msg != null && msg.get("content") != null) {
                        return (String) msg.get("content");
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to template content on API failure
        }
        return null;
    }
}
