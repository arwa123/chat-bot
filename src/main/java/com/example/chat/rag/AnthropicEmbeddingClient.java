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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;

/**
 * Client for generating embeddings using Anthropic's API
 */
@Component
@Primary
public class AnthropicEmbeddingClient implements EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicEmbeddingClient.class);
    private static final String EMBEDDING_ENDPOINT = "/seldon/seldon/bge-m3-79ebf/v2/models/bge-m3-79ebf/infer";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private final WebClient webClient;
    private final String baseUrl;


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


    @Override
    public List<Double> getEmbeddings(String text) {
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
    

    public List<List<Double>> getEmbeddingsForMultipleTexts(List<String> texts) {
        try {
            EmbeddingRequest request = EmbeddingRequest.forMultipleTexts(texts);
            return getEmbeddingsInternal(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embeddings for texts", e);
        }
    }
    

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
                    .block();

            if (response == null) {
                logger.error("Null response from embedding API");
                throw new RuntimeException("Null response received from embedding API");
            }
            return response.getEmbeddings();
            
        } catch (Exception e) {
            logger.error("Error calling embedding API", e);
            throw new RuntimeException("Error calling embedding API: " + e.getMessage(), e);
        }
    }
}