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
    

    private String toVectorLiteral(List<Double> vector) {
        return "[" + vector.stream()
            .map(d -> String.format(java.util.Locale.US, "%.6f", d))
            .collect(Collectors.joining(",")) + "]";
    }
    
    @Override
    @Transactional
    public UUID store(EmbeddedChunk chunk) throws StorageException {
        try {
            Map<String, Object> enhancedMetadata = new HashMap<>(chunk.metadata());
            if (chunk.documentId() != null) {
                enhancedMetadata.put("document_id", chunk.documentId().toString());
            }
            enhancedMetadata.put("chunk_index", chunk.chunkIndex());
            if (chunk.embeddingModel() != null) {
                enhancedMetadata.put("embedding_model", chunk.embeddingModel());
            }
            String metadataJson = objectMapper.writeValueAsString(enhancedMetadata);
            String vectorLiteral = toVectorLiteral(chunk.embedding());
            jdbcTemplate.update(INSERT_CHUNK_SQL,
                    chunk.id(),
                    chunk.source(),
                    chunk.content(),
                    metadataJson,
                    vectorLiteral);
            
            return chunk.id();
        } catch (DataAccessException | JsonProcessingException e) {
            logger.error("Error storing embedded chunk: {}", e.getMessage(), e);
            throw new StorageException("Failed to store embedded chunk: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<UUID> storeBatch(List<EmbeddedChunk> chunks) throws StorageException {
        try {
            if (chunks.size() <= batchSize) {
                return storeBatchSequential(chunks);
            }
            return storeBatchParallel(chunks);
        } catch (Exception e) {
            logger.error("Error batch storing embedded chunks: {}", e.getMessage(), e);
            throw new StorageException("Failed to store embedded chunks in batch: " + e.getMessage(), e);
        }
    }


    private List<UUID> storeBatchSequential(List<EmbeddedChunk> chunks) throws StorageException {
        List<UUID> ids = new ArrayList<>();
        for (EmbeddedChunk chunk : chunks) {
            store(chunk);
            ids.add(chunk.id());
        }
        return ids;
    }


    public CompletableFuture<List<UUID>> storeBatchAsync(List<EmbeddedChunk> chunks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storeBatch(chunks);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }, storageExecutor);
    }

    private List<UUID> storeBatchParallel(List<EmbeddedChunk> chunks) throws StorageException {
        logger.debug("Processing {} chunks in parallel sub-batches of size {}", chunks.size(), batchSize);
        
        int effectiveBatchSize = chunks.size() > 1000 ? Math.min(batchSize, 10) : batchSize;
        
        List<UUID> allIds = new ArrayList<>(chunks.size());
        int totalBatches = (chunks.size() + effectiveBatchSize - 1) / effectiveBatchSize;
        int completedBatches = 0;
        
        for (int i = 0; i < chunks.size(); i += effectiveBatchSize) {
            int end = Math.min(i + effectiveBatchSize, chunks.size());
            List<EmbeddedChunk> batch = chunks.subList(i, end);
            
            try {
                List<UUID> batchIds = storeBatchSequential(batch);
                allIds.addAll(batchIds);
                completedBatches++;
                
                if (completedBatches % 5 == 0 || completedBatches == totalBatches) {
                    logger.info("Stored batch {}/{} ({} chunks)", 
                            completedBatches, totalBatches, allIds.size());
                }
                
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

