package com.example.chat.ingestion.embedding;

/**
 * Exception thrown when embedding generation fails
 */
public class EmbeddingException extends Exception {
    
    public EmbeddingException(String message) {
        super(message);
    }
    
    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}