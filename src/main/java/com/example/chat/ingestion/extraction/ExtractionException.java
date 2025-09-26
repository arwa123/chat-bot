package com.example.chat.ingestion.extraction;

/**
 * Exception thrown when text extraction fails
 */
public class ExtractionException extends Exception {

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}