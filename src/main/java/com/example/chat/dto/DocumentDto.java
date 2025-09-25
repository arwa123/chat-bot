package com.example.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * DTOs for document ingestion and processing operations
 */
public class DocumentDto {


    /**
     * Response for document ingestion operations
     */
    public record DocumentResponse(
            UUID documentId,
            int chunkCount,
            String message,
            boolean success
    ) {
        public static DocumentResponse success(UUID documentId, int chunkCount, String message) {
            return new DocumentResponse(documentId, chunkCount, message, true);
        }

        public static DocumentResponse error(String message) {
            return new DocumentResponse(null, 0, message, false);
        }
    }
    
    /**
     * Configuration options for document processing
     */
    public record DocumentProcessingOptions(
            Integer chunkSize,
            Integer overlapSize,
            String chunkType,
            String embeddingProvider
    ) {}
}