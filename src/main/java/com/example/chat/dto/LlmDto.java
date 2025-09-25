package com.example.chat.dto;

import java.util.List;

/**
 * DTOs for LLM-related operations
 * Using Java Records for immutable data transfer objects
 */
public class LlmDto {

    /**
     * Request for text generation
     */
    public record GenerationRequest(
        String prompt,
        List<EmbeddingDto.RetrievedKnowledge> context,
        Double temperature,
        Integer maxTokens
    ) {}

    /**
     * Response from text generation
     */
    public record GenerationResponse(
        String text,
        String model,
        boolean augmented
    ) {
        // Static factory method to replace builder
        public static GenerationResponse success(String text, String model, boolean augmented) {
            return new GenerationResponse(text, model, augmented);
        }
        
        public static GenerationResponse error(String errorMessage) {
            return new GenerationResponse(errorMessage, null, false);
        }
    }

    /**
     * Represents an error in LLM processing
     */
    public record LlmError(
        String errorType,
        String message,
        String requestId
    ) {}
}