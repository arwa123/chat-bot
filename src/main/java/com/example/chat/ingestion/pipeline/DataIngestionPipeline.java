package com.example.chat.ingestion.pipeline;


import com.example.chat.ingestion.chunking.TextChunker;
import com.example.chat.ingestion.embedding.EmbeddingService;
import com.example.chat.ingestion.factory.EmbeddingServiceFactory;
import com.example.chat.ingestion.factory.TextChunkerFactory;
import com.example.chat.ingestion.storage.PostgresVectorStorage;
import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.TextChunk;
import com.example.chat.ingestion.storage.VectorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.UUID;

/**
 * Main service for document ingestion pipeline.
 * Orchestrates the flow from document to vector storage.
 * Supports parallel processing using CompletableFuture.
 */
@Service
public class DataIngestionPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(DataIngestionPipeline.class);
    
    private final TextChunkerFactory chunkerFactory;
    private final EmbeddingServiceFactory embeddingServiceFactory;
    private final VectorStorage vectorStorage;

    private final ExecutorService chunkingExecutor;
    private final ExecutorService embeddingExecutor;
    private final ExecutorService storageExecutor;
    
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;

    public DataIngestionPipeline(
            TextChunkerFactory chunkerFactory,
            EmbeddingServiceFactory embeddingServiceFactory,
            VectorStorage vectorStorage,
            @Value("${rag.chunking.default-type:fixed-size}") String defaultChunkType,
            @Value("${rag.chunking.default-chunk-size:1000}") int defaultChunkSize,
            @Value("${rag.chunking.default-overlap-size:100}") int defaultOverlapSize,
            @Value("${rag.embedding.provider:anthropic}") String defaultEmbeddingProvider,
            @Value("${rag.pipeline.thread-pool-size:5}") int threadPoolSize) {
        
        this.chunkerFactory = chunkerFactory;
        this.embeddingServiceFactory = embeddingServiceFactory;
        this.vectorStorage = vectorStorage;
        int poolSize = threadPoolSize > 0 ? threadPoolSize : DEFAULT_THREAD_POOL_SIZE;
        this.chunkingExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "chunking-pool-thread");
            t.setDaemon(true);
            return t;
        });
        
        this.embeddingExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "embedding-pool-thread");
            t.setDaemon(true);
            return t;
        });
        
        this.storageExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "storage-pool-thread");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized data ingestion pipeline with default chunking type: {}, " +
                "chunk size: {}, overlap: {}, embedding provider: {}, thread pool size: {}", 
                defaultChunkType, defaultChunkSize, defaultOverlapSize, defaultEmbeddingProvider, poolSize);
    }
    

    /**
     * Process a document through the complete ingestion pipeline asynchronously.
     * The document goes through three stages:
     * 1. Chunking - Breaking the document into manageable text chunks
     * 2. Embedding - Converting text chunks into vector embeddings
     * 3. Storage - Storing the embeddings in the vector database
     *
     * @param document The document to process
     * @return CompletableFuture that will resolve to a list of stored chunk IDs
     */
    public CompletableFuture<List<UUID>> processDataAsync(Document document) {
         return chunkTextAsync(document)
                .thenComposeAsync(this::generateEmbeddingsAsync, embeddingExecutor)
                .thenComposeAsync(this::storeEmbeddingsAsync, storageExecutor);
    }

    
    /**
     * Chunk extracted text into smaller pieces asynchronously
     * 
     * @param content The extracted content to chunk
     * @return CompletableFuture that will resolve to a list of text chunks
     */
    public CompletableFuture<List<TextChunk>> chunkTextAsync(Document content) {
        try {
            logger.debug("Chunking text asynchronously from: {}", content.metadata());
            TextChunker chunker = chunkerFactory.getChunker();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return chunker.chunk(content);
                } catch (Exception e) {
                    throw new CompletionException(new PipelineException(
                            "Text chunking failed: " + e.getMessage(), e));
                }
            }, chunkingExecutor);
        } catch (Exception e) {
            CompletableFuture<List<TextChunk>> future = new CompletableFuture<>();
            future.completeExceptionally(new PipelineException("Text chunking failed: " + e.getMessage(), e));
            return future;
        }
    }

    /**
     * Generate embeddings for text chunks asynchronously with parallel processing
     * 
     * @param chunks The text chunks to embed
     * @return CompletableFuture that will resolve to a list of embedded chunks
     */
    public CompletableFuture<List<EmbeddedChunk>> generateEmbeddingsAsync(List<TextChunk> chunks) {
        final int batchSize = 50;
        EmbeddingService embeddingService = embeddingServiceFactory.getEmbeddingService();
        if (chunks.size() <= batchSize) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return embeddingService.embedBatch(chunks);
                } catch (Exception e) {
                    throw new CompletionException(new PipelineException(
                            "Embedding generation failed: " + e.getMessage(), e));
                }
            }, embeddingExecutor);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<EmbeddedChunk> results = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, chunks.size());
                    List<TextChunk> batch = chunks.subList(i, end);
                    List<EmbeddedChunk> batchResults = embeddingService.embedBatch(batch);
                    results.addAll(batchResults);
                }
                return results;
            } catch (Exception e) {
                throw new CompletionException(new PipelineException(
                        "Embedding generation failed: " + e.getMessage(), e));
            }
        }, embeddingExecutor);
    }
    
    /**
     * Store embedded chunks in vector database asynchronously with parallel processing
     * 
     * @param chunks The embedded chunks to store
     * @return CompletableFuture that will resolve to a list of stored chunk IDs
     */
    public CompletableFuture<List<UUID>> storeEmbeddingsAsync(List<EmbeddedChunk> chunks) {
        try {
             return ((PostgresVectorStorage) vectorStorage).storeBatchAsync(chunks);
        } catch (Exception e) {
            logger.error("Vector storage failed: {}", e.getMessage());
            CompletableFuture<List<UUID>> future = new CompletableFuture<>();
            future.completeExceptionally(new PipelineException("Vector storage failed: " + e.getMessage(), e));
            return future;
        }
    }
}