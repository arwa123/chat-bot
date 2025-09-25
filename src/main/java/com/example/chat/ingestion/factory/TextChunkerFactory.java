package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.chunking.ChunkingConfig;
import com.example.chat.ingestion.chunking.FixedSizeTextChunker;
import com.example.chat.ingestion.chunking.TextChunker;
import com.example.chat.ingestion.model.ExtractedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory for creating TextChunker instances based on content and configuration.
 */
@Component
public class TextChunkerFactory {
    

    @Autowired
    FixedSizeTextChunker fixedSizeTextChunker;

    public TextChunker getChunker() {
       return fixedSizeTextChunker;
    }
    

}