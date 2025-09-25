package com.example.chat.repository;

import com.example.chat.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> { }
