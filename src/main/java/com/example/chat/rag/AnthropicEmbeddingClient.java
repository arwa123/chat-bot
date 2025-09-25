package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class AnthropicEmbeddingClient implements EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicEmbeddingClient.class);
    
    @Value("${rag.embedding.dimension}")
    private int embeddingDimension;

    private final WebClient webClient;
    private final String baseUrl;
    private static final String EMBEDDING_URL = "/seldon/seldon/bge-m3-79ebf/v2/models/bge-m3-79ebf/infer";

    public AnthropicEmbeddingClient(
            @Value("${rag.generation.anthropic.api-key}") String apiKey,
            @Value("${rag.generation.anthropic.base-url}") String baseUrl) {
        
        this.baseUrl = baseUrl;
        // Disable SSL verification for dev environment
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
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
                .baseUrl(this.baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Generate embeddings for a single text
     */
    @Override
    public List<Double> getEmbeddings(String text) {
        return getEmbeddings(Arrays.asList(text)).get(0);
    }

    /**
     * Generate embeddings for multiple texts
     */
    public List<List<Double>> getEmbeddings(List<String> texts) {
        logger.debug("Generating embeddings for {} texts", texts.size());
        try {
            // Create payload matching the Python format
            Map<String, Object> payload = Map.of(
                    "inputs", List.of(
                            Map.of(
                                    "name", "input",
                                    "shape", List.of(texts.size()),
                                    "datatype", "BYTES",
                                    "data", texts
                            )
                    )
            );

            Map<String, Object> response = webClient.post()
                    .uri(EMBEDDING_URL)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Parse response
            if (response != null && response.containsKey("outputs")) {
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) response.get("outputs");
                if (!outputs.isEmpty()) {
                    Map<String, Object> output = outputs.get(0);
                    if (output.containsKey("data")) {
                        Object data = output.get("data");
                        List<List<Double>> embeddingData = new ArrayList<>();
                        
                        // Handle different possible formats of the data
                        if (data instanceof List) {
                            List<?> dataList = (List<?>) data;
                            if (!dataList.isEmpty()) {
                                if (dataList.get(0) instanceof List) {
                                    // Format is already List<List<Double>>
                                    embeddingData = (List<List<Double>>) data;
                                } else if (dataList.get(0) instanceof Double) {
                                    // Format is List<Double>, convert to List<List<Double>>
                                    embeddingData.add((List<Double>) dataList);
                                }
                            }
                        }
                        
                        return embeddingData;
                    }
                }
            }

            logger.error("Unexpected response format from embedding API");
            throw new RuntimeException("Unexpected response format from embedding API");

        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.error("Embedding API error: {} - Response: {} (Status: {})", 
                    e.getMessage(), responseBody, e.getStatusCode());
            throw new RuntimeException("Embedding API error: " + e.getMessage() +
                    " Response: " + responseBody + " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            logger.error("Error calling embedding API", e);
            throw new RuntimeException("Error calling embedding API: " + e.getMessage(), e);
        }
    }


    /**
     * Calculate cosine similarity between two embeddings
     */
    public double cosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += embedding1.get(i) * embedding1.get(i);
            norm2 += embedding2.get(i) * embedding2.get(i);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}