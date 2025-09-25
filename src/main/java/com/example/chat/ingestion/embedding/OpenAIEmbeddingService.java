package com.example.chat.ingestion.embedding;

import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.TextChunk;
import com.example.chat.rag.OpenAIEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI implementation of the EmbeddingService interface.
 * Uses OpenAI's embedding API to generate vector representations of text.
 */
@Service
public class OpenAIEmbeddingService implements EmbeddingService {

    @Override
    public String getModelName() {
        return "";
    }

    @Override
    public List<EmbeddedChunk> embedBatch(List<TextChunk> chunks)  {
        return List.of();
    }
}