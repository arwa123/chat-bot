package com.example.chat.controller;

import com.example.chat.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/v1/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimiter rateLimiter;

    @GetMapping("/status")
    public ResponseEntity<?> getRateLimitStatus(HttpServletRequest request) {
        String apiKey = request.getHeader("x-api-key");
        if (apiKey == null) {
            return ResponseEntity.status(401).body(Map.of("error", "API key is required"));
        }
        
        int limit = rateLimiter.getRateLimit(apiKey);
        int remaining = rateLimiter.getRemainingRequests(apiKey);
        long resetTimeSeconds = rateLimiter.getResetTimeMs(apiKey) / 1000;
        
        return ResponseEntity.ok(Map.of(
            "limit", limit,
            "remaining", remaining,
            "reset_in_seconds", resetTimeSeconds
        ));
    }
    
    @PostMapping("/configure")
    public ResponseEntity<?> setCustomRateLimit(
            HttpServletRequest request,
            @RequestParam String targetApiKey,
            @RequestParam int requestsPerMinute) {
        
        // Admin API key would be required to change limits
        String adminApiKey = request.getHeader("x-api-key");
        if (adminApiKey == null) {
            return ResponseEntity.status(401).body(Map.of("error", "API key is required"));
        }
        
        // In a real implementation, you would check if the current API key has admin privileges
        // For simplicity, we'll allow any authenticated user to set limits in this demo
        
        rateLimiter.setCustomRateLimit(targetApiKey, requestsPerMinute);
        
        return ResponseEntity.ok(Map.of(
            "message", "Rate limit updated successfully",
            "api_key", targetApiKey,
            "limit", requestsPerMinute
        ));
    }
}