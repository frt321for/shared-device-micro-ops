package com.iot.ops.application.module.dispatch.api;

import com.iot.ops.application.module.dispatch.domain.Route;
import com.iot.ops.application.module.dispatch.service.DispatchService;
import com.iot.ops.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @PostMapping("/priorities")
    public ApiResponse<List<Map<String, Object>>> calculatePriorities() {
        List<Map<String, Object>> priorities = dispatchService.calculatePriorities();
        return ApiResponse.success(priorities);
    }

    @PostMapping("/routes")
    public ApiResponse<Route> generateRoute(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> workOrderIds = ((List<Integer>) body.get("workOrderIds"))
                .stream().map(Long::valueOf).toList();
        Long assigneeId = Long.valueOf(body.get("assigneeId").toString());
        Route route = dispatchService.generateRoute(workOrderIds, assigneeId);
        return ApiResponse.created(route);
    }

    @GetMapping("/routes/{id}")
    public ApiResponse<Map<String, Object>> getRoute(@PathVariable Long id) {
        Map<String, Object> result = dispatchService.getRoute(id);
        if (result == null) {
            return ApiResponse.error(404, "Route not found");
        }
        return ApiResponse.success(result);
    }

    @PutMapping("/routes/{id}")
    public ApiResponse<Route> adjustRoute(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> workOrderIds = ((List<Integer>) body.get("workOrderIds"))
                .stream().map(Long::valueOf).toList();
        Route route = dispatchService.adjustRoute(id, workOrderIds);
        return ApiResponse.success(route);
    }

    @GetMapping("/routes/active")
    public ApiResponse<List<Route>> getActiveRoutes() {
        List<Route> routes = dispatchService.getActiveRoutes();
        return ApiResponse.success(routes);
    }
}
