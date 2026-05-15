package com.iot.ops.application.module.ai.api;

import com.iot.ops.application.module.ai.domain.WeeklyReport;
import com.iot.ops.application.module.ai.service.AiService;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/replenishment-note")
    public ApiResponse<String> generateReplenishmentNote(@RequestBody Map<String, Long> body) {
        Long deviceId = body.get("deviceId");
        if (deviceId == null) {
            return ApiResponse.error(400, "deviceId is required");
        }
        return ApiResponse.success(aiService.generateReplenishmentNote(deviceId));
    }

    @PostMapping("/fault-analysis")
    public ApiResponse<String> generateFaultAnalysis(@RequestBody Map<String, Long> body) {
        Long deviceId = body.get("deviceId");
        if (deviceId == null) {
            return ApiResponse.error(400, "deviceId is required");
        }
        return ApiResponse.success(aiService.generateFaultAnalysis(deviceId));
    }

    @PostMapping("/weekly-report")
    public ApiResponse<WeeklyReport> generateWeeklyReport(@RequestBody Map<String, Object> body) {
        Long siteId = body.get("siteId") != null ? ((Number) body.get("siteId")).longValue() : null;
        LocalDate periodStart = body.get("periodStart") != null
                ? LocalDate.parse((String) body.get("periodStart")) : null;
        LocalDate periodEnd = body.get("periodEnd") != null
                ? LocalDate.parse((String) body.get("periodEnd")) : null;

        if (siteId == null || periodStart == null || periodEnd == null) {
            return ApiResponse.error(400, "siteId, periodStart, periodEnd are required");
        }
        return ApiResponse.success(aiService.generateWeeklyReport(siteId, periodStart, periodEnd));
    }

    @GetMapping("/weekly-reports")
    public ApiResponse<List<WeeklyReport>> getWeeklyReports(@RequestParam(required = false) Long siteId) {
        if (siteId == null) {
            return ApiResponse.error(400, "siteId is required");
        }
        return ApiResponse.success(aiService.getWeeklyReports(siteId));
    }

    @GetMapping("/weekly-reports/{id}")
    public ApiResponse<WeeklyReport> getWeeklyReport(@PathVariable Long id) {
        return ApiResponse.success(aiService.getWeeklyReport(id));
    }
}
