package com.example.ragchat.repository;

import com.example.ragchat.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> { }
