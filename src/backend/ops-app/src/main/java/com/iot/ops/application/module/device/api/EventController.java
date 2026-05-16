package com.iot.ops.application.module.device.api;

import com.iot.ops.application.infra.mqtt.DeviceEventMessageHandler;
import com.iot.ops.application.module.device.domain.DeviceEvent;
import com.iot.ops.application.module.device.repository.DeviceEventRepository;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final MessageChannel mqttInputChannel;
    private final DeviceRepository deviceRepository;
    private final DeviceEventRepository deviceEventRepository;

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

        Optional<com.iot.ops.application.module.device.domain.Device> deviceOpt =
            deviceRepository.findByDeviceCode(deviceCode);
        if (deviceOpt.isPresent()) {
            String severity = (String) body.getOrDefault("severity", "info");
            if (body.get("severity") == null && "fault".equals(eventType)) {
                severity = "high";
            }
            DeviceEvent event = DeviceEvent.builder()
                .deviceId(deviceOpt.get().getId())
                .eventType(eventType)
                .eventData(payload)
                .severity(severity)
                .occurredAt(LocalDateTime.now())
                .build();
            deviceEventRepository.save(event);
        }

        return ApiResponse.success("Event " + eventType + " sent to " + deviceCode);
    }
}
