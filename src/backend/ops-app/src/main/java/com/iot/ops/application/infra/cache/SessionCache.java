package com.iot.ops.application.infra.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionCache {

    private static final String PREFIX = "session:";
    private final StringRedisTemplate redis;

    public SessionCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void set(String token, String username, long ttlSeconds) {
        redis.opsForValue().set(PREFIX + token, username, Duration.ofSeconds(ttlSeconds));
    }

    public String get(String token) {
        return redis.opsForValue().get(PREFIX + token);
    }

    public void remove(String token) {
        redis.delete(PREFIX + token);
    }
}
