package com.example.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final Set<String> validKeys = new HashSet<>();

    public ApiKeyFilter(@Value("${security.api-keys}") String keys) {
        for (String k : keys.split(",")) {
            if (!k.isBlank()) validKeys.add(k.trim());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // allow swagger docs and health without key (optional)
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }
        String apiKey = request.getHeader("x-api-key");
        if (apiKey == null || !validKeys.contains(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("Invalid or missing API key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
