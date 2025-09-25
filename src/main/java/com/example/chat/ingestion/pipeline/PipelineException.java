package com.example.chat.ingestion.pipeline;

/**
 * Exception thrown when any step in the data ingestion pipeline fails
 */
public class PipelineException extends Exception {
    
    public PipelineException(String message) {
        super(message);
    }
    
    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}