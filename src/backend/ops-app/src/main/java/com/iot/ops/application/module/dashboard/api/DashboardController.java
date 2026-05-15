package com.iot.ops.application.module.dashboard.api;

import com.iot.ops.application.module.dashboard.service.DashboardService;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.success(dashboardService.getOverview());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<Map<String, Object>>> getAlerts() {
        return ApiResponse.success(dashboardService.getAlerts());
    }

    @GetMapping("/device-stats")
    public ApiResponse<Map<String, Object>> getDeviceStats() {
        return ApiResponse.success(dashboardService.getDeviceStats());
    }

    @GetMapping("/today-tasks")
    public ApiResponse<List<WorkOrder>> getTodayTasks() {
        return ApiResponse.success(dashboardService.getTodayTasks());
    }
}
