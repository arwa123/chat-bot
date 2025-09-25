package com.example.ragchat.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

@Component("anthropicLlmClient")
@Primary
@ConditionalOnProperty(name = "rag.generation.provider", havingValue = "anthropic")
public class AnthropicLlmClient implements LlmClient {

    private final WebClient webClient;

    @Value("${rag.generation.anthropic.model:claude-3-5-sonnet-20241022}")
    private String model;

    @Value("${rag.generation.anthropic.base-url:}")
    private String baseUrl;

    public AnthropicLlmClient(
            @Value("${rag.generation.anthropic.api-key}") String apiKey) {
        try {
            // Create insecure SSL context for dev environment
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup WebClient with SSL bypass", e);
        }
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 512,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            Map<String, Object> response = webClient.post()
                    .uri("/cosmosai/llm/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("choices");
                if (!content.isEmpty() && content.get(0).containsKey("message")) {
                    Map<String, Object> msg = (Map<String, Object>) content.get(0).get("message");
                    System.out.println(msg.get("content"));
                    return (String) msg.get("content");
                }
            }

            throw new RuntimeException("Unexpected response format from Claude API");

        } catch (WebClientResponseException e) {
            throw new RuntimeException(" Claude API error: " + e.getMessage() +
                    " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Claude API: " + e.getMessage(), e);
        }
    }
}