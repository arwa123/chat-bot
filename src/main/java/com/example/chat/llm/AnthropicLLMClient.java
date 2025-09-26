package com.example.chat.llm;

import com.example.chat.dto.LLMDto;
import com.example.chat.dto.LLMDto.AnthropicChatRequest;
import com.example.chat.dto.LLMDto.AnthropicChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;

@Component("anthropicLlmClient")
@Primary
@ConditionalOnProperty(name = "llm.generation.provider", havingValue = "anthropic")
public class AnthropicLLMClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicLLMClient.class);
    private static final String API_ENDPOINT = "/cosmosai/llm/v1/chat/completions";
    private static final int DEFAULT_MAX_TOKENS = 1024;
    private static final double DEFAULT_TEMPERATURE = 0.0;

    private final WebClient webClient;
    private final String baseUrl;

    @Value("${llm.generation.anthropic.model}")
    private String model;

    public AnthropicLLMClient(
            @Value("${llm.generation.anthropic.api-key}") String apiKey,
            @Value("${llm.generation.anthropic.base-url}") String baseUrl) {
        
        this.baseUrl = baseUrl;
        logger.info("Initializing Anthropic LLM client with model: {}", model);

        // Bypass SSL check only on DEV
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
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
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
        
        logger.debug("Anthropic LLM client initialized successfully");
    }
    
    @Override
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            logger.warn("Empty prompt provided to Anthropic LLM client");
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        
        try {
            logger.debug("Generating text with Anthropic API, prompt length: {}", prompt.length());
            

            AnthropicChatRequest request = AnthropicChatRequest
                    .builder()
                    .model(model)
                    .messages(List.of(new LLMDto.Message("user", prompt)))
                    .max_tokens(DEFAULT_MAX_TOKENS)
                    .temperature(DEFAULT_TEMPERATURE)
                    .build();
            

            AnthropicChatResponse response = webClient.post()
                    .uri(API_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AnthropicChatResponse.class)
                    .block();
            

            if (response == null) {
                logger.error("Null response received from Anthropic API");
                throw new RuntimeException("Null response received from Anthropic API");
            }
            
            String content = response.getContent();
            if (content == null || content.isBlank()) {
                logger.error("Empty content received from Anthropic API");
                throw new RuntimeException("Empty content received from Anthropic API");
            }
            
            logger.debug("Successfully generated text with Anthropic API, response length: {}", content.length());
            return content;

        } catch (WebClientResponseException e) {
            logger.error("Anthropic API error: {} (Status: {})", e.getMessage(), e.getStatusCode());
            throw new RuntimeException("Anthropic API error: " + e.getMessage() +
                    " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            logger.error("Error calling Anthropic API", e);
            throw new RuntimeException("Error calling Anthropic API: " + e.getMessage(), e);
        }
    }
}