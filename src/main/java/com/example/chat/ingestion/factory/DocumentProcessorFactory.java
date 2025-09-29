package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.chunking.FixedSizeTextChunker;
import com.example.chat.ingestion.document.DocumentProcessor;
import com.example.chat.ingestion.document.TxtDocumentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Factory for creating appropriate DocumentProcessor instances based on file type.
 */
@Component
public class DocumentProcessorFactory {

    @Autowired
    TxtDocumentProcessor processor;


    public DocumentProcessor getProcessor() {
        return processor;
    }
}