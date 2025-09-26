package com.example.chat.ingestion.embedding;

import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.TextChunk;
import com.example.chat.rag.AnthropicEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Anthropic implementation of the EmbeddingService interface.
 * Uses Anthropic's embedding API to generate vector representations of text.
 */
@Service
@Primary
public class AnthropicEmbeddingService implements EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnthropicEmbeddingService.class);

    private final AnthropicEmbeddingClient embeddingClient;
    private final String modelName;
    private final ExecutorService embeddingExecutor;
    private final int batchSize;
    
    @Autowired
    public AnthropicEmbeddingService(
            AnthropicEmbeddingClient embeddingClient,
            @Value("${rag.embedding.model}") String modelName,
            @Value("${rag.pipeline.thread-pool-size:5}") int threadPoolSize,
            @Value("${rag.embedding.batch-size:10}") int batchSize) {
        this.embeddingClient = embeddingClient;
        this.modelName = modelName;
        this.batchSize = batchSize > 0 ? batchSize : 10;
        
        int poolSize = threadPoolSize > 0 ? threadPoolSize : 5;
        this.embeddingExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "embedding-worker-thread");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized Anthropic embedding service with model {}, thread pool size {}, and batch size {}",
                modelName, poolSize, this.batchSize);
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public List<EmbeddedChunk> embedBatch(List<TextChunk> chunks) throws EmbeddingException {
        try {
            logger.debug("Generating embeddings for {} chunks in batch", chunks.size());
            
            if (chunks.isEmpty()) {
                return new ArrayList<>();
            }
            return embedBatchParallel(chunks);
            
        } catch (Exception e) {
            logger.error("Error batch generating embeddings: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embeddings in batch", e);
        }
    }
    

    private List<EmbeddedChunk> embedBatchInternal(List<TextChunk> chunks) throws Exception {
        List<String> texts = chunks.stream()
                .map(TextChunk::content)
                .collect(Collectors.toList());
        
        List<List<Double>> embeddings = embeddingClient.getEmbeddingsForMultipleTexts(texts);
        
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            List<Double> embedding = embeddings.get(i);
            
            embeddedChunks.add(EmbeddedChunk.builder()
                    .id(chunk.id())
                    .documentId(chunk.documentId())
                    .source(chunk.source())
                    .content(chunk.content())
                    .metadata(chunk.metadata())
                    .chunkIndex(chunk.chunkIndex())
                    .embedding(embedding)
                    .embeddingModel(getModelName())
                    .build());
        }
        
        return embeddedChunks;
    }
    

    private List<EmbeddedChunk> embedBatchParallel(List<TextChunk> chunks) {
        logger.debug("Processing {} chunks in parallel sub-batches of size {}", chunks.size(), batchSize);
        
        List<CompletableFuture<List<EmbeddedChunk>>> batchFutures = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<TextChunk> batch = chunks.subList(i, end);
            
            batchFutures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return embedBatchInternal(batch);
                } catch (Exception e) {
                    logger.error("Error generating embeddings for batch: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to generate embeddings for batch", e);
                }
            }, embeddingExecutor));
        }
        
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> batchFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))
                .join();
    }
}