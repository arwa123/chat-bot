package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.TextChunk;
import java.util.List;

public interface TextChunker {
    

    String getType();

    List<TextChunk> chunk(Document content);

    default boolean supports(Document content) {
        return true;
    }
}