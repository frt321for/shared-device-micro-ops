package com.iot.ops.application.module.workorder.api;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
import com.iot.ops.application.module.workorder.service.WorkOrderService;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public ApiResponse<Page<WorkOrder>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(workOrderService.findAll(status, type, assigneeId, siteId, page, size));
    }

    @PostMapping
    public ApiResponse<WorkOrder> create(@RequestBody WorkOrder workOrder) {
        return ApiResponse.success(workOrderService.create(workOrder));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrder> detail(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.findById(id));
    }

    @PutMapping("/{id}/assign")
    public ApiResponse<WorkOrder> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        return ApiResponse.success(workOrderService.assign(id, body.get("assigneeId")));
    }

    @PutMapping("/{id}/arrive")
    public ApiResponse<WorkOrder> arrive(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.arrive(id));
    }

    @PutMapping("/{id}/process")
    public ApiResponse<WorkOrder> process(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.process(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<WorkOrder> complete(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return ApiResponse.success(workOrderService.complete(id, body.get("actualQty")));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<WorkOrder> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.success(workOrderService.review(id, body.get("reviewResult"), body.get("remark")));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<WorkOrder> cancel(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.cancel(id));
    }

    @GetMapping("/{id}/audit")
    public ApiResponse<List<WorkOrderAudit>> audit(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.getAuditLogs(id));
    }
}
