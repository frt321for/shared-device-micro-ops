package com.iot.ops.application.module.revenue.api;

import com.iot.ops.application.module.revenue.domain.SiteRevenue;
import com.iot.ops.application.module.revenue.service.RevenueService;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/sites")
    public ApiResponse<List<SiteRevenue>> getSiteRankings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(revenueService.getSiteRankings(startDate, endDate));
    }

    @GetMapping("/sites/{id}")
    public ApiResponse<SiteRevenue> getSiteDetail(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(revenueService.getSiteDetail(id, startDate, endDate));
    }

    @GetMapping("/devices")
    public ApiResponse<List<Map<String, Object>>> getDeviceEfficiency() {
        return ApiResponse.success(revenueService.getDeviceEfficiency());
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.success(revenueService.getOverview());
    }
}
