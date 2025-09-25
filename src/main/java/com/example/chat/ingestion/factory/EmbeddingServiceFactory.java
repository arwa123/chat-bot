package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.embedding.AnthropicEmbeddingService;
import com.example.chat.ingestion.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating EmbeddingService instances based on provider configuration.
 */
@Component
public class EmbeddingServiceFactory {


    @Autowired
    AnthropicEmbeddingService anthropicEmbeddingService;

    public EmbeddingService getEmbeddingService() {
        return anthropicEmbeddingService;
    }


}