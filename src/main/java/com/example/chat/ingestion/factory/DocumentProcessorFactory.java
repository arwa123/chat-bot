package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.document.DocumentProcessor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Factory for creating appropriate DocumentProcessor instances based on file type.
 */
@Component
public class DocumentProcessorFactory {
    

    private final List<DocumentProcessor> processors;
    
    public DocumentProcessorFactory(List<DocumentProcessor> processors) {
        this.processors = processors;
    }
    

    public DocumentProcessor getProcessor(String contentType) {
        return processors.stream()
                .filter(processor -> processor.supports(contentType))
                .findFirst().get();
    }
}