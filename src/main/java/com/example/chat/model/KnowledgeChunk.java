package com.example.chat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@NamedNativeQueries({
    @NamedNativeQuery(
        name = "KnowledgeChunk.findSimilarByVector",
        query = "SELECT id, content, source, metadata, (embedding <=> :vector\\:\\:vector) AS distance " +
                "FROM knowledge_chunks " +
                "ORDER BY embedding <=> :vector\\:\\:vector " +
                "LIMIT :limit",
        resultSetMapping = "KnowledgeChunkWithDistanceMapping"
    ),
    @NamedNativeQuery(
        name = "KnowledgeChunk.findSimilarByVectorWithOperation",
        query = "SELECT id, content, source, metadata, (embedding :operation :vector\\:\\:vector) AS distance " +
                "FROM knowledge_chunks " +
                "ORDER BY embedding :operation :vector\\:\\:vector " +
                "LIMIT :limit",
        resultSetMapping = "KnowledgeChunkWithDistanceMapping"
    )
})
@SqlResultSetMapping(
    name = "KnowledgeChunkWithDistanceMapping",
    classes = {
        @ConstructorResult(
            targetClass = KnowledgeChunkWithDistance.class,
            columns = {
                @ColumnResult(name = "id", type = UUID.class),
                @ColumnResult(name = "content", type = String.class),
                @ColumnResult(name = "source", type = String.class),
                @ColumnResult(name = "metadata", type = String.class),
                @ColumnResult(name = "distance", type = Double.class)
            }
        )
    }
)
public class KnowledgeChunk {
    @Id
    private UUID id;
    private String source;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(columnDefinition = "jsonb")
    private String metadata;
    // The embedding field is not directly mapped by JPA as it's a Postgres-specific vector type
    // We'll handle it through native queries
}
