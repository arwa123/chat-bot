package com.example.chat.ingestion.storage;

import com.example.chat.ingestion.model.EmbeddedChunk;

import java.util.List;
import java.util.UUID;


public interface VectorStorage {
    

    UUID store(EmbeddedChunk chunk) throws StorageException;

    List<UUID> storeBatch(List<EmbeddedChunk> chunks) throws StorageException;

}