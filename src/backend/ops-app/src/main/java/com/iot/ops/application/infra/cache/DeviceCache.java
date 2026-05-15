package com.iot.ops.application.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceCache {

    private static final String KEY_HEARTBEAT = "device:%s:heartbeat";
    private static final String KEY_STATUS = "device:%s:status";
    private static final String KEY_STOCK = "device:%s:stock:%d";
    private static final String KEY_FAULT = "device:%s:faults";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public DeviceCache(StringRedisTemplate redis) {
        this.redis = redis;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public void updateHeartbeat(String deviceCode, LocalDateTime time) {
        redis.opsForValue().set(
            KEY_HEARTBEAT.formatted(deviceCode),
            time.toString(),
            Duration.ofMinutes(5)
        );
        redis.opsForValue().set(
            KEY_STATUS.formatted(deviceCode),
            "online",
            Duration.ofMinutes(5)
        );
    }

    public boolean isOnline(String deviceCode) {
        String val = redis.opsForValue().get(KEY_HEARTBEAT.formatted(deviceCode));
        return val != null;
    }

    public void updateStock(String deviceCode, Long skuId, int quantity) {
        String key = KEY_STOCK.formatted(deviceCode, skuId);
        redis.opsForValue().set(key, String.valueOf(quantity), Duration.ofHours(2));
    }

    public Integer getStock(String deviceCode, Long skuId) {
        String val = redis.opsForValue().get(KEY_STOCK.formatted(deviceCode, skuId));
        return val != null ? Integer.parseInt(val) : null;
    }

    public void recordFault(String deviceCode, String faultCode) {
        String key = KEY_FAULT.formatted(deviceCode);
        redis.opsForHash().increment(key, faultCode, 1);
        redis.expire(key, Duration.ofDays(7));
        redis.opsForValue().set(KEY_STATUS.formatted(deviceCode), "fault", Duration.ofMinutes(5));
    }

    public Map<Object, Object> getFaults(String deviceCode) {
        return redis.opsForHash().entries(KEY_FAULT.formatted(deviceCode));
    }
}
