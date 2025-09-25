package com.example.chat.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component("geminiLlmClient")
@ConditionalOnProperty(name = "rag.generation.provider", havingValue = "gemini")
public class GeminiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final String apiKey;

    @Value("${rag.generation.gemini.model:gemini-1.5-pro}")
    private String model;

    public GeminiLlmClient(
            @Value("${rag.generation.gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 512,
                            "temperature", 0.7
                    )
            );

            Map<String, Object> response = webClient.post()
                    .uri("/v1beta/models/" + model + ":generateContent?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    if (candidate.containsKey("content")) {
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        if (content.containsKey("parts")) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                                return (String) parts.get(0).get("text");
                            }
                        }
                    }
                }
            }

            throw new RuntimeException("Unexpected response format from Gemini API");

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Gemini API error: " + e.getMessage() +
                    " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }
}