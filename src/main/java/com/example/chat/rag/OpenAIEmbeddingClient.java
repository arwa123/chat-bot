package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * OpenAI embeddings client implementation.
 * Uses the OpenAI API to generate embeddings for text.
 */
@Component("openaiEmbeddingClient")
public class OpenAIEmbeddingClient implements EmbeddingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingClient.class);
    private final WebClient webClient;

    @Value("${rag.embedding.dimension:1024}")
    private int embeddingDimension;
    
    @Value("${rag.embedding.model:text-embedding-3-small}")
    private String model;

    public OpenAIEmbeddingClient(@Value("${rag.embedding.openai-api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenAI API key is not configured. This client will not work without a valid API key.");
        }
        
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    @Override
    public List<Double> getEmbeddings(String text) {
        if (text == null || text.isBlank()) {
            logger.warn("Empty text provided for embedding");
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        try {
            logger.debug("Generating OpenAI embeddings for text of length: {}", text.length());
            
            // This is a placeholder - in a real implementation, you would:
            // 1. Create a request to the OpenAI embeddings API
            // 2. Parse the response to extract the embedding
            // 3. Return the embedding as a List<Double>
            
            logger.warn("OpenAI embedding client not fully implemented. Using stub implementation would be better.");
            throw new UnsupportedOperationException("OpenAI embedding client not implemented in this starter. Use stub or implement API call.");
            
            /* Example implementation (commented out):
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", text,
                "encoding_format", "float"
            );
            
            Map<String, Object> response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                    
            // Extract embedding from response
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            
            return embedding;
            */
            
        } catch (WebClientResponseException e) {
            logger.error("OpenAI API error: {} (Status: {})", e.getMessage(), e.getStatusCode());
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error generating OpenAI embeddings", e);
            throw new RuntimeException("Error generating OpenAI embeddings: " + e.getMessage(), e);
        }
    }
}
