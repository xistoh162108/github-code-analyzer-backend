package com.backend.githubanalyzer.infra.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisDao {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> values;

    public RedisDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.values = redisTemplate.opsForValue();
    }

    public void setValues(String key, Object value) {
        values.set(key, value);
    }

    public void setValues(String key, Object value, Duration duration) {
        values.set(key, value, duration);
    }

    public Object getValues(String key) {
        return values.get(key);
    }

    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
