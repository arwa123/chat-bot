package com.example.chat.repository;

import com.example.chat.model.KnowledgeChunk;
import com.example.chat.model.KnowledgeChunkWithDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {
    

    @Query(nativeQuery = true, name = "KnowledgeChunk.findSimilarByVector")
    List<KnowledgeChunkWithDistance> findSimilarByVector(
            @Param("vector") String vector,
            @Param("limit") int limit);
    

    @Query(nativeQuery = true, value = 
            "INSERT INTO knowledge_chunks(id, source, content, metadata, embedding) " +
            "VALUES (:id, :source, :content, CAST(:metadata AS jsonb), :embedding\\:\\:vector) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "    source = EXCLUDED.source, " +
            "    content = EXCLUDED.content, " +
            "    metadata = EXCLUDED.metadata, " +
            "    embedding = EXCLUDED.embedding")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void upsertWithEmbedding(
            @Param("id") UUID id,
            @Param("source") String source,
            @Param("content") String content,
            @Param("metadata") String metadata,
            @Param("embedding") String embedding);
}
