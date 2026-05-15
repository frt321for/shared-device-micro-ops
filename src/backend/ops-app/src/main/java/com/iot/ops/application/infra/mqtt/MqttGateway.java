package com.iot.ops.application.infra.mqtt;

import com.iot.ops.application.module.simulator.MqttEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MqttGateway {
    private final MqttEventPublisher eventPublisher;

    public void publish(String deviceCode, String eventType, Map<String, Object> data) {
        eventPublisher.publish(deviceCode, eventType, data);
    }
}
