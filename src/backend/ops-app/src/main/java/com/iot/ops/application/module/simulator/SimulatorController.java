package com.iot.ops.application.module.simulator;

import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final DeviceSimulator deviceSimulator;

    @PostMapping("/start")
    public ApiResponse<String> start() {
        deviceSimulator.start();
        return ApiResponse.success("模拟器已启动");
    }

    @PostMapping("/stop")
    public ApiResponse<String> stop() {
        deviceSimulator.stop();
        return ApiResponse.success("模拟器已停止");
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of("running", deviceSimulator.isRunning()));
    }
}
