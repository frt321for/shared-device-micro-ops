package com.iot.ops.application.infra.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeviceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventPublisher.class);
    private static final String TOPIC_PREFIX = "iot/ops/device";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MessageChannel mqttInputChannel;

    public void publish(String deviceCode, String eventType, Map<String, Object> data) {
        try {
            String topic = TOPIC_PREFIX + "/" + deviceCode + "/" + eventType;
            String payload = mapper.writeValueAsString(data);
            var message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();
            mqttInputChannel.send(message);
            log.debug("Dispatched {} event for device {}", eventType, deviceCode);
        } catch (Exception e) {
            log.error("Failed to publish device event: {}", e.getMessage());
        }
    }
}
