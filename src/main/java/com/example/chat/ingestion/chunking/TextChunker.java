package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.ExtractedContent;
import com.example.chat.ingestion.model.TextChunk;

import java.util.List;

/**
 * Interface for chunking extracted text content into smaller pieces.
 * Different implementations can use different strategies for chunking.
 */
public interface TextChunker {
    
    /**
     * Type of chunking strategy (e.g., "fixed-size", "semantic", "paragraph")
     * 
     * @return String identifier for the chunking strategy
     */
    String getType();
    
    /**
     * Splits the extracted content into smaller chunks
     * 
     * @param content The extracted content to chunk
     * @return List of text chunks
     */
    List<TextChunk> chunk(ExtractedContent content);
    
    /**
     * Checks if this chunker is suitable for the given content
     * 
     * @param content The extracted content
     * @return true if this chunker can process the content, false otherwise
     */
    default boolean supports(ExtractedContent content) {
        return true;
    }
}