package com.example.chat.ingestion.pipeline;


import com.example.chat.ingestion.chunking.TextChunker;
import com.example.chat.ingestion.embedding.EmbeddingService;
import com.example.chat.ingestion.extraction.ExtractionException;
import com.example.chat.ingestion.extraction.TextExtractor;
import com.example.chat.ingestion.factory.EmbeddingServiceFactory;
import com.example.chat.ingestion.factory.TextChunkerFactory;
import com.example.chat.ingestion.factory.TextExtractorFactory;
import com.example.chat.ingestion.storage.PostgresVectorStorage;
import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.ingestion.model.ExtractedContent;
import com.example.chat.ingestion.model.TextChunk;
import com.example.chat.ingestion.storage.VectorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main service for document ingestion pipeline.
 * Orchestrates the flow from document to vector storage.
 * Supports parallel processing using CompletableFuture.
 */
@Service
public class DataIngestionPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(DataIngestionPipeline.class);
    
    private final TextExtractorFactory extractorFactory;
    private final TextChunkerFactory chunkerFactory;
    private final EmbeddingServiceFactory embeddingServiceFactory;
    private final VectorStorage vectorStorage;

    private final ExecutorService chunkingExecutor;
    private final ExecutorService embeddingExecutor;
    private final ExecutorService storageExecutor;
    
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;

    public DataIngestionPipeline(
            TextExtractorFactory extractorFactory,
            TextChunkerFactory chunkerFactory,
            EmbeddingServiceFactory embeddingServiceFactory,
            VectorStorage vectorStorage,
            @Value("${rag.chunking.default-type:fixed-size}") String defaultChunkType,
            @Value("${rag.chunking.default-chunk-size:1000}") int defaultChunkSize,
            @Value("${rag.chunking.default-overlap-size:100}") int defaultOverlapSize,
            @Value("${rag.embedding.provider:anthropic}") String defaultEmbeddingProvider,
            @Value("${rag.pipeline.thread-pool-size:5}") int threadPoolSize) {
        
        this.extractorFactory = extractorFactory;
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
    

    public CompletableFuture<List<UUID>> processDataAsync(
            Document document) {
        
        logger.info("Processing document asynchronously: {}", document.filename());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractText(document);
            } catch (Exception e) {
                throw new CompletionException(new PipelineException(
                        "Failed to extract text: " + e.getMessage(), e));
            }
        }).thenComposeAsync(extractedContent -> {
            return chunkTextAsync(extractedContent);
        }, chunkingExecutor).thenComposeAsync(chunks -> {
            return generateEmbeddingsAsync(chunks);
        }, embeddingExecutor).thenComposeAsync(embeddedChunks -> {
            return storeEmbeddingsAsync(embeddedChunks);
        }, storageExecutor).whenComplete((chunkIds, throwable) -> {
            if (throwable != null) {
                logger.error("Error in async document processing pipeline: {}", 
                        throwable.getMessage(), throwable);
            } else {
                logger.info("Successfully processed document asynchronously: {}, created {} chunks", 
                        document.filename(), chunkIds.size());
            }
        });
    }
    
    /**
     * Extract text content from a document
     * 
     * @param document The document to extract text from
     * @return Extracted content
     * @throws PipelineException if extraction fails
     */
    public ExtractedContent extractText(Document document) throws PipelineException {
        try {
            logger.debug("Extracting text from document: {}", document.filename());
            
            Optional<TextExtractor> extractorOpt = extractorFactory.getExtractor(document.contentType());
            
            if (extractorOpt.isEmpty()) {
                throw new PipelineException("No suitable text extractor found for content type: " + 
                        document.contentType());
            }
            
            TextExtractor extractor = extractorOpt.get();
            return extractor.extract(document);
        } catch (ExtractionException e) {
            logger.error("Error extracting text: {}", e.getMessage(), e);
            throw new PipelineException("Text extraction failed: " + e.getMessage(), e);
        }
    }
    

    
    /**
     * Chunk extracted text into smaller pieces asynchronously
     * 
     * @param content The extracted content to chunk
     * @return CompletableFuture that will resolve to a list of text chunks
     */
    public CompletableFuture<List<TextChunk>> chunkTextAsync(ExtractedContent content) {
        try {
            logger.debug("Chunking text asynchronously from: {}", content.source());
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
        if (chunks.isEmpty()) {
            logger.warn("No chunks to embed");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        logger.debug("Generating embeddings asynchronously for {} chunks", chunks.size());
        
        // Process in smaller batches to avoid memory issues
        final int batchSize = 50; // Reduce batch size to manage memory better
        
        if (chunks.size() <= batchSize) {
            // For small number of chunks, process in a single batch
            return CompletableFuture.supplyAsync(() -> {
                try {
                    EmbeddingService embeddingService = embeddingServiceFactory.getEmbeddingService();
                    return embeddingService.embedBatch(chunks);
                } catch (Exception e) {
                    throw new CompletionException(new PipelineException(
                            "Embedding generation failed: " + e.getMessage(), e));
                }
            }, embeddingExecutor);
        }
        
        // For larger sets, we need to split into smaller batches
        return CompletableFuture.supplyAsync(() -> {
            try {
                EmbeddingService embeddingService = embeddingServiceFactory.getEmbeddingService();
                List<EmbeddedChunk> results = new ArrayList<>();
                
                // Process in smaller batches
                for (int i = 0; i < chunks.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, chunks.size());
                    List<TextChunk> batch = chunks.subList(i, end);
                    
                    // Process each batch and collect results
                    List<EmbeddedChunk> batchResults = embeddingService.embedBatch(batch);
                    results.addAll(batchResults);
                    
                    // Force garbage collection periodically to free memory
                    if (i > 0 && i % (batchSize * 5) == 0) {
                        System.gc();
                    }
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
        if (chunks.isEmpty()) {
            logger.warn("No embedded chunks to store");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        logger.debug("Storing {} embedded chunks in vector database asynchronously", chunks.size());
        try {
                return ((PostgresVectorStorage) vectorStorage).storeBatchAsync(chunks);
        } catch (Exception e) {
            CompletableFuture<List<UUID>> future = new CompletableFuture<>();
            future.completeExceptionally(new PipelineException("Vector storage failed: " + e.getMessage(), e));
            return future;
        }
    }
}