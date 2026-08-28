package com.enterprise.iam.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;
    
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String ATTEMPT_PREFIX = "login_attempts:";
    private static final String LOCK_PREFIX = "login_locked:";

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void loginSucceeded(String key) {
        redisTemplate.delete(ATTEMPT_PREFIX + key);
        redisTemplate.delete(LOCK_PREFIX + key);
    }

    public void loginFailed(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts != null && attempts == 1) {
            // Expire attempts after 1 hour if not locked
            redisTemplate.expire(attemptKey, Duration.ofHours(1));
        }
        
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            String lockKey = LOCK_PREFIX + key;
            redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_DURATION);
            redisTemplate.delete(attemptKey);
        }
    }

    public boolean isBlocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}
