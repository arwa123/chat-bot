package com.example.chat.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {
    private final Map<String, ApiKeyUsage> usageMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> customLimits = new ConcurrentHashMap<>();
    private final int defaultMaxRequestsPerMinute;

    public RateLimiter(@Value("${security.rate-limit-per-minute:120}") int defaultMaxRequestsPerMinute) {
        this.defaultMaxRequestsPerMinute = defaultMaxRequestsPerMinute;
    }
    

    public int getRateLimit(String apiKey) {
        return customLimits.getOrDefault(apiKey, defaultMaxRequestsPerMinute);
    }

    public boolean allowRequest(String apiKey) {
        long now = System.currentTimeMillis();
        ApiKeyUsage usage = usageMap.computeIfAbsent(apiKey, k -> new ApiKeyUsage(now));
        int limit = getRateLimit(apiKey);

        synchronized (usage) {
            if (now - usage.windowStartTime > Duration.ofMinutes(1).toMillis()) {
                usage.resetWindow(now);
            }
            int count = usage.incrementAndGet();
            return count <= limit;
        }
    }


    public int getRemainingRequests(String apiKey) {
        ApiKeyUsage usage = usageMap.get(apiKey);
        int limit = getRateLimit(apiKey);
        
        if (usage == null) {
            return limit;
        }
        
        synchronized (usage) {
            long now = System.currentTimeMillis();
            if (now - usage.windowStartTime > Duration.ofMinutes(1).toMillis()) {
                usage.resetWindow(now);
                return limit;
            }
            
            return Math.max(0, limit - usage.requestCount.get());
        }
    }


    public long getResetTimeMs(String apiKey) {
        ApiKeyUsage usage = usageMap.get(apiKey);
        if (usage == null) {
            return 0;
        }
        
        synchronized (usage) {
            long now = System.currentTimeMillis();
            long resetTime = usage.windowStartTime + Duration.ofMinutes(1).toMillis();
            return Math.max(0, resetTime - now);
        }
    }

    private static class ApiKeyUsage {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStartTime;

        public ApiKeyUsage(long startTime) {
            this.windowStartTime = startTime;
        }

        public void resetWindow(long newStartTime) {
            requestCount.set(0);
            windowStartTime = newStartTime;
        }

        public int incrementAndGet() {
            return requestCount.incrementAndGet();
        }
    }
}