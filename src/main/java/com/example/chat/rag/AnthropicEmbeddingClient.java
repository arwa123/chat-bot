package com.example.chat.rag;

import com.example.chat.dto.EmbeddingDto.EmbeddingRequest;
import com.example.chat.dto.EmbeddingDto.EmbeddingResponse;
import com.example.chat.dto.EmbeddingDto.EmbeddingError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Client for generating embeddings using Anthropic's API
 */
@Component
@Primary
public class AnthropicEmbeddingClient implements EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicEmbeddingClient.class);
    private static final String EMBEDDING_ENDPOINT = "/seldon/seldon/bge-m3-79ebf/v2/models/bge-m3-79ebf/infer";
    private static final int EMBEDDING_DIMENSION = 1024;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final int RETRY_MIN_BACKOFF_SECONDS = 1;

    private final WebClient webClient;
    private final String baseUrl;

    /**
     * Creates a new Anthropic embedding client
     * 
     * @param apiKey API key for authentication
     * @param baseUrl Base URL for the embedding API
     */
    public AnthropicEmbeddingClient(
            @Value("${rag.embedding.api-key}") String apiKey,
            @Value("${rag.embedding.base-url}") String baseUrl) {
        
        this.baseUrl = baseUrl;
        logger.info("Initializing Anthropic embedding client with base URL: {}", baseUrl);

        HttpClient httpClient = configureHttpClient();

        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        logger.debug("Anthropic embedding client initialized successfully");
    }


    private HttpClient configureHttpClient() {
        return HttpClient.create()
                .responseTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .secure(sslContextSpec -> {
                    try {
                        sslContextSpec.sslContext(
                                io.netty.handler.ssl.SslContextBuilder.forClient()
                                        .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                                        .build()
                        );
                    } catch (SSLException e) {
                        logger.error("Error configuring SSL context", e);
                        throw new RuntimeException("Error configuring SSL context", e);
                    }
                });
    }

    /**
     * Generate embeddings for a single text
     * 
     * @param text Text to embed
     * @return Vector representation as a list of doubles
     */
    @Override
    public List<Double> getEmbeddings(String text) {
        if (text == null || text.isBlank()) {
            logger.warn("Empty text provided for embedding");
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        logger.debug("Generating embedding for text of length: {}", text.length());
        
        try {
            EmbeddingRequest request = EmbeddingRequest.forSingleText(text);
            List<List<Double>> embeddings = getEmbeddingsInternal(request);

            if (embeddings.isEmpty()) {
                throw new RuntimeException("No embeddings returned from API");
            }
            
            return embeddings.get(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding for text", e);
        }
    }
    
    /**
     * Generate embeddings for multiple texts
     * 
     * @param texts List of texts to embed
     * @return List of embedding vectors
     */
    public List<List<Double>> getEmbeddingsForMultipleTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            logger.warn("Empty text list provided for embeddings");
            return Collections.emptyList();
        }
        
        logger.debug("Generating embeddings for {} texts", texts.size());
        
        try {
            // Create request using factory method
            EmbeddingRequest request = EmbeddingRequest.forMultipleTexts(texts);
            
            // Get embeddings
            return getEmbeddingsInternal(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embeddings for texts", e);
        }
    }
    
    /**
     * Internal method to get embeddings from the API
     * 
     * @param request The embedding request
     * @return List of embedding vectors
     */
    private List<List<Double>> getEmbeddingsInternal(EmbeddingRequest request) {
        try {

            EmbeddingResponse response = webClient.post()
                    .uri(EMBEDDING_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        statusCode -> statusCode.is4xxClientError() || statusCode.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(EmbeddingError.class)
                            .flatMap(error -> {
                                String errorMsg = error != null ? error.message() : "Unknown error";
                                return Mono.<Throwable>error(new ResponseStatusException(
                                    clientResponse.statusCode(),
                                    "API error: " + errorMsg
                                ));
                            })
                    )
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_MIN_BACKOFF_SECONDS))
                            .filter(throwable -> !(throwable instanceof TimeoutException))
                    )
                    .block();

            if (response == null) {
                logger.error("Null response from embedding API");
                throw new RuntimeException("Null response received from embedding API");
            }
            
            List<List<Double>> embeddings = response.getEmbeddings();
            if (embeddings == null) {
                logger.error("No embeddings returned from API");
                throw new RuntimeException("No embeddings returned from API");
            }
            
            if (embeddings.isEmpty()) {
                logger.warn("Empty embeddings list returned from API, returning empty list");
                return Collections.emptyList();
            }
            
            for (List<Double> embedding : embeddings) {
                if (embedding.size() != EMBEDDING_DIMENSION) {
                    logger.error("Unexpected embedding dimension: got {}, expected {}", 
                            embedding.size(), EMBEDDING_DIMENSION);
                    throw new IllegalStateException(
                            "Unexpected embedding dimension: got " + embedding.size() + 
                            ", expected " + EMBEDDING_DIMENSION);
                }
            }
            
            logger.debug("Successfully generated {} embeddings", embeddings.size());
            return embeddings;
            
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
}