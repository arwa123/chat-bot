package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.extraction.TextExtractor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class TextExtractorFactory {
    
    private final List<TextExtractor> extractors;
    
    public TextExtractorFactory(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }

    public Optional<TextExtractor> getExtractor(String contentType) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(contentType))
                .findFirst();
    }
}