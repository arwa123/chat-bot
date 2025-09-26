package com.example.chat.ingestion.embedding;

import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.TextChunk;
import org.springframework.stereotype.Service;
import java.util.List;


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