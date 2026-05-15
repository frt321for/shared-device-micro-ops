package com.iot.ops.application.module.device.api;

import com.iot.ops.application.infra.mqtt.DeviceEventMessageHandler;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final MessageChannel mqttInputChannel;

    @PostMapping("/{deviceCode}/{eventType}")
    public ApiResponse<String> postEvent(
            @PathVariable String deviceCode,
            @PathVariable String eventType,
            @RequestBody Map<String, Object> body) {
        String topic = "iot/ops/device/" + deviceCode + "/" + eventType;
        String payload = body.toString();
        var message = MessageBuilder.withPayload(payload)
            .setHeader("mqtt_receivedTopic", topic)
            .build();
        mqttInputChannel.send(message);
        return ApiResponse.success("Event " + eventType + " sent to " + deviceCode);
    }
}
