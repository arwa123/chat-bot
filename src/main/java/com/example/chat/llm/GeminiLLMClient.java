package com.example.chat.llm;

import com.example.chat.dto.LLMDto.GeminiRequest;
import com.example.chat.dto.LLMDto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component("geminiLlmClient")
@ConditionalOnProperty(name = "rag.generation.provider", havingValue = "gemini")
public class GeminiLLMClient implements LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiLLMClient.class);
    private static final String API_ENDPOINT = "/v1beta/models/";
    private static final String PATH_VARIABLE = ":generateContent?key=";

    private final WebClient webClient;
    private final String apiKey;

    @Value("${rag.generation.gemini.model}")
    private String model;

    public GeminiLLMClient(
            @Value("${rag.generation.gemini.api-key}") String apiKey,
            @Value("${rag.generation.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        logger.info("Initializing Gemini LLM client with model: {}", model);
        
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
                
        logger.debug("Gemini LLM client initialized successfully");
    }


    @Override
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            logger.warn("Empty prompt provided to Gemini LLM client");
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        
        try {
            logger.debug("Generating text with Gemini API, prompt length: {}", prompt.length());
            
            
            GeminiRequest request = GeminiRequest.create(prompt);
            
            GeminiResponse response = webClient.post()
                    .uri(API_ENDPOINT + model + PATH_VARIABLE + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();
            
            
            if (response == null) {
                logger.error("Null response received from Gemini API");
                throw new RuntimeException("Null response received from Gemini API");
            }
            
            String text = response.getText();
            if (text == null || text.isBlank()) {
                logger.error("Empty text received from Gemini API");
                throw new RuntimeException("Empty text received from Gemini API");
            }
            
            logger.debug("Successfully generated text with Gemini API, response length: {}", text.length());
            return text;

        } catch (WebClientResponseException e) {
            logger.error("Gemini API error: {} (Status: {})", e.getMessage(), e.getStatusCode());
            throw new RuntimeException("Gemini API error: " + e.getMessage() +
                    " (Status: " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }
    
}