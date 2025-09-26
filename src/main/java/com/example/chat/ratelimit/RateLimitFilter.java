package com.example.chat.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Order(1) // Execute before ApiKeyFilter
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip rate limiting for docs and health endpoints
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader("x-api-key");
        if (apiKey == null) {
            // Let the ApiKeyFilter handle missing API keys
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check rate limit
        if (!rateLimiter.allowRequest(apiKey)) {
            response.setStatus(429);
            response.setContentType("application/json");
            
            // Get remaining time until reset
            int limit = rateLimiter.getRateLimit(apiKey);
            long resetTimeMs = rateLimiter.getResetTimeMs(apiKey);
            
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTimeMs / 1000));
            
            // Write error response
            Map<String, Object> errorResponse = Map.of(
                "error", "Too Many Requests",
                "message", "API rate limit exceeded. You are allowed " + limit + " requests per minute.",
                "limit", limit,
                "reset_in_seconds", resetTimeMs / 1000
            );
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }
        
        // Add rate limit headers to successful responses
        int limit = rateLimiter.getRateLimit(apiKey);
        int remaining = rateLimiter.getRemainingRequests(apiKey);
        long resetTimeMs = rateLimiter.getResetTimeMs(apiKey);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTimeMs / 1000));
        
        filterChain.doFilter(request, response);
    }
}