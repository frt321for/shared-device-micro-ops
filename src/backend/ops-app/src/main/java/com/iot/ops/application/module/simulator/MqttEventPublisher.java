package com.iot.ops.application.module.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MqttEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttEventPublisher.class);
    private static final String TOPIC_PREFIX = "iot/ops/device";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MessageChannel mqttInputChannel;

    public void publish(String deviceCode, String eventType, Map<String, Object> data) {
        try {
            String topic = TOPIC_PREFIX + "/" + deviceCode + "/" + eventType;
            String payload = mapper.writeValueAsString(data);
            Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();
            mqttInputChannel.send(message);
            log.debug("Dispatched {} event for {}", eventType, deviceCode);
        } catch (Exception e) {
            log.error("Failed to dispatch event: {}", e.getMessage());
        }
    }
}
