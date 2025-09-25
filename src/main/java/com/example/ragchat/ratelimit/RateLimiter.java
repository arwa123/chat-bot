package com.example.ragchat.ratelimit;

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
    
    /**
     * Set a custom rate limit for a specific API key
     * 
     * @param apiKey The API key to set the limit for
     * @param requestsPerMinute The maximum number of requests per minute for this key
     */
    public void setCustomRateLimit(String apiKey, int requestsPerMinute) {
        if (requestsPerMinute > 0) {
            customLimits.put(apiKey, requestsPerMinute);
        } else {
            customLimits.remove(apiKey);
        }
    }
    
    /**
     * Get the rate limit for a specific API key
     * 
     * @param apiKey The API key to get the limit for
     * @return The maximum number of requests per minute for this key
     */
    public int getRateLimit(String apiKey) {
        return customLimits.getOrDefault(apiKey, defaultMaxRequestsPerMinute);
    }

    /**
     * Check if the API key has exceeded rate limits
     * 
     * @param apiKey The API key to check
     * @return true if request should be allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String apiKey) {
        long now = System.currentTimeMillis();
        ApiKeyUsage usage = usageMap.computeIfAbsent(apiKey, k -> new ApiKeyUsage(now));
        int limit = getRateLimit(apiKey);

        synchronized (usage) {
            // Reset counter if minute window has elapsed
            if (now - usage.windowStartTime > Duration.ofMinutes(1).toMillis()) {
                usage.resetWindow(now);
            }
            
            // Increment counter and check against limit
            int count = usage.incrementAndGet();
            return count <= limit;
        }
    }

    /**
     * Get the number of remaining requests for this API key in the current window
     */
    public int getRemainingRequests(String apiKey) {
        ApiKeyUsage usage = usageMap.get(apiKey);
        int limit = getRateLimit(apiKey);
        
        if (usage == null) {
            return limit;
        }
        
        synchronized (usage) {
            long now = System.currentTimeMillis();
            // Reset counter if minute window has elapsed
            if (now - usage.windowStartTime > Duration.ofMinutes(1).toMillis()) {
                usage.resetWindow(now);
                return limit;
            }
            
            return Math.max(0, limit - usage.requestCount.get());
        }
    }

    /**
     * Get the time in milliseconds until the rate limit resets for this API key
     */
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