package com.example.chat.ingestion.storage;

import com.example.chat.ingestion.embedding.AnthropicEmbeddingService;
import com.example.chat.ingestion.factory.EmbeddingServiceFactory;
import com.example.chat.ingestion.model.EmbeddedChunk;
import com.example.chat.repository.KnowledgeChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of VectorStorage using PostgreSQL with pgvector extension.
 */
@Repository
public class PostgresVectorStorage implements VectorStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgresVectorStorage.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddingServiceFactory embeddingServiceFactory;
    private final ExecutorService storageExecutor;
    private final int batchSize;


    @Autowired
    KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    AnthropicEmbeddingService embeddingService;

    private static final String INSERT_CHUNK_SQL = 
            "INSERT INTO knowledge_chunks(id, source, content, metadata, embedding) " +
            "VALUES (?, ?, ?, CAST(? AS jsonb), ?::vector) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "    source = EXCLUDED.source, " +
            "    content = EXCLUDED.content, " +
            "    metadata = EXCLUDED.metadata, " +
            "    embedding = EXCLUDED.embedding";
            
    private static final String FIND_SIMILAR_SQL = 
            "SELECT id, content, source, metadata, " +
            "       (embedding <=> ?::vector) AS distance " +
            "FROM knowledge_chunks " +
            "ORDER BY embedding <=> ?::vector " +
            "LIMIT ?";
    
    public PostgresVectorStorage(JdbcTemplate jdbcTemplate, 
                                ObjectMapper objectMapper,
                                EmbeddingServiceFactory embeddingServiceFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.embeddingServiceFactory = embeddingServiceFactory;
        this.batchSize = 20;
        int poolSize =  5;
        this.storageExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "storage-worker-thread");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized Postgres vector storage with thread pool size {} and batch size {}",
                poolSize, this.batchSize);
    }
    
    /**
     * Convert a list of doubles to a PostgreSQL vector literal string
     * 
     * @param vector List of vector components
     * @return String in PostgreSQL vector format [x,y,z,...]
     */
    private String toVectorLiteral(List<Double> vector) {
        return "[" + vector.stream()
            .map(d -> String.format(java.util.Locale.US, "%.6f", d))
            .collect(Collectors.joining(",")) + "]";
    }
    
    @Override
    @Transactional
    public UUID store(EmbeddedChunk chunk) throws StorageException {
        try {
            logger.debug("Storing embedded chunk with ID: {}", chunk.getId());
            
            // Ensure document ID, chunk index, and embedding model info are stored in metadata
            Map<String, Object> enhancedMetadata = new HashMap<>(chunk.getMetadata());
            
            if (chunk.getDocumentId() != null) {
                enhancedMetadata.put("document_id", chunk.getDocumentId().toString());
            }
            
            enhancedMetadata.put("chunk_index", chunk.getChunkIndex());
            
            if (chunk.getEmbeddingModel() != null) {
                enhancedMetadata.put("embedding_model", chunk.getEmbeddingModel());
            }
            
            String metadataJson = objectMapper.writeValueAsString(enhancedMetadata);
            String vectorLiteral = toVectorLiteral(chunk.getEmbedding());
            
            jdbcTemplate.update(INSERT_CHUNK_SQL,
                    chunk.getId(),
                    chunk.getSource(),
                    chunk.getContent(),
                    metadataJson,
                    vectorLiteral);
            
            return chunk.getId();
        } catch (DataAccessException | JsonProcessingException e) {
            logger.error("Error storing embedded chunk: {}", e.getMessage(), e);
            throw new StorageException("Failed to store embedded chunk: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<UUID> storeBatch(List<EmbeddedChunk> chunks) throws StorageException {
        try {
            logger.debug("Storing {} embedded chunks in batch", chunks.size());

            if (chunks.isEmpty()) {
                return new ArrayList<>();
            }

            // For small batches, use the standard approach
            if (chunks.size() <= batchSize) {
                return storeBatchSequential(chunks);
            }

            // For larger batches, process in parallel
            return storeBatchParallel(chunks);

        } catch (Exception e) {
            logger.error("Error batch storing embedded chunks: {}", e.getMessage(), e);
            throw new StorageException("Failed to store embedded chunks in batch: " + e.getMessage(), e);
        }
    }

    /**
     * Store a small batch of embedded chunks sequentially
     *
     * @param chunks The embedded chunks to store
     * @return List of stored chunk IDs
     */
    private List<UUID> storeBatchSequential(List<EmbeddedChunk> chunks) throws StorageException {
        List<UUID> ids = new ArrayList<>();
        for (EmbeddedChunk chunk : chunks) {
            store(chunk);
            ids.add(chunk.getId());
        }
        return ids;
    }

    /**
     * Store embedded chunks in parallel using CompletableFuture
     * 
     * @param chunks The embedded chunks to store
     * @return CompletableFuture that will resolve to a list of stored chunk IDs
     */
    public CompletableFuture<List<UUID>> storeBatchAsync(List<EmbeddedChunk> chunks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storeBatch(chunks);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }, storageExecutor);
    }
    
    /**
     * Process a large batch of chunks in parallel sub-batches
     * 
     * @param chunks The embedded chunks to store
     * @return List of stored chunk IDs
     */
    private List<UUID> storeBatchParallel(List<EmbeddedChunk> chunks) throws StorageException {
        logger.debug("Processing {} chunks in parallel sub-batches of size {}", chunks.size(), batchSize);
        
        // Use a smaller batch size for large inputs to manage memory better
        int effectiveBatchSize = chunks.size() > 1000 ? Math.min(batchSize, 10) : batchSize;
        
        // Process in sequential batches to avoid OutOfMemoryError
        List<UUID> allIds = new ArrayList<>(chunks.size());
        int totalBatches = (chunks.size() + effectiveBatchSize - 1) / effectiveBatchSize;
        int completedBatches = 0;
        
        // Process in manageable chunks to avoid memory issues
        for (int i = 0; i < chunks.size(); i += effectiveBatchSize) {
            int end = Math.min(i + effectiveBatchSize, chunks.size());
            List<EmbeddedChunk> batch = chunks.subList(i, end);
            
            // Process current batch
            try {
                List<UUID> batchIds = storeBatchSequential(batch);
                allIds.addAll(batchIds);
                completedBatches++;
                
                // Log progress periodically
                if (completedBatches % 5 == 0 || completedBatches == totalBatches) {
                    logger.info("Stored batch {}/{} ({} chunks)", 
                            completedBatches, totalBatches, allIds.size());
                }
                
                // Force garbage collection periodically to free memory
                if (completedBatches % 10 == 0) {
                    System.gc();
                }
                
            } catch (StorageException e) {
                logger.error("Error storing batch of chunks {}-{}: {}", i, end, e.getMessage(), e);
                throw new StorageException("Failed to store batch of chunks: " + e.getMessage(), e);
            }
        }
        
        return allIds;
    }
}

