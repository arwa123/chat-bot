package com.example.chat.llm;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;

import java.util.List;
import java.util.Map;

@Component("anthropicLlmClient")
@Primary
@ConditionalOnProperty(name = "rag.generation.provider", havingValue = "anthropic")
public class AnthropicLlmClient implements LlmClient {

    private final WebClient webClient;

    @Value("${rag.generation.anthropic.model}")
    private String model;

    public AnthropicLlmClient(
            @Value("${rag.generation.anthropic.api-key}") String apiKey,
        @Value("${rag.generation.anthropic.base-url}") String baseurl){
        // Completely disable SSL verification for dev
        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec ->
                        {
                            try {
                                sslContextSpec.sslContext(
                                        io.netty.handler.ssl.SslContextBuilder.forClient()
                                                .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                                                .build()
                                );
                            } catch (SSLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

        this.webClient = WebClient.builder()
                .baseUrl(baseurl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)  // Back to Bearer
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 128,
                    "temperature", 0,
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
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message.containsKey("content")) {
                            return (String) message.get("content");
                        }
                    }
                }
            }

            throw new RuntimeException("Unexpected response format from Claude API");

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Claude API error: " + e.getMessage() +
                    " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Claude API: " + e.getMessage(), e);
        }
    }
}