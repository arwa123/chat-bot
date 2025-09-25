package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.extraction.TextExtractor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory for creating TextExtractor instances based on content type.
 * Uses the Chain of Responsibility pattern to find an appropriate extractor.
 */
@Component
public class TextExtractorFactory {
    
    private final List<TextExtractor> extractors;
    
    public TextExtractorFactory(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }
    
    /**
     * Get a TextExtractor that supports the given content type
     * 
     * @param contentType MIME type of the document
     * @return TextExtractor that can handle the document type, or empty if none found
     */
    public Optional<TextExtractor> getExtractor(String contentType) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(contentType))
                .findFirst();
    }
}