package com.example.chat.ingestion.embedding;

import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.TextChunk;

import java.util.List;

public interface EmbeddingService {
    

    String getModelName();
    

    List<EmbeddedChunk> embedBatch(List<TextChunk> chunks) throws EmbeddingException;
}