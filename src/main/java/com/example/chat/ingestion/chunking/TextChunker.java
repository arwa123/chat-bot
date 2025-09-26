package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.ExtractedContent;
import com.example.chat.ingestion.model.TextChunk;
import java.util.List;

public interface TextChunker {
    

    String getType();

    List<TextChunk> chunk(ExtractedContent content);

    default boolean supports(ExtractedContent content) {
        return true;
    }
}