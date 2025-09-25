package com.example.chat.ingestion.storage;

import com.example.chat.ingestion.model.EmbeddedChunk;

import java.util.List;
import java.util.UUID;

/**
 * Interface for storing and retrieving embedded text chunks in a vector database.
 * Implementations will handle specific vector databases like Postgres with pgvector.
 */
public interface VectorStorage {
    
    /**
     * Store a single embedded chunk in the vector database
     * 
     * @param chunk The embedded chunk to store
     * @return ID of the stored chunk
     * @throws StorageException if storage operation fails
     */
    UUID store(EmbeddedChunk chunk) throws StorageException;
    
    /**
     * Store multiple embedded chunks in a batch operation
     * 
     * @param chunks List of embedded chunks to store
     * @return List of IDs for the stored chunks
     * @throws StorageException if storage operation fails
     */
    List<UUID> storeBatch(List<EmbeddedChunk> chunks) throws StorageException;

}