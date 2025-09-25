package com.example.ragchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class Dtos {

    @Data
    public static class CreateSessionReq { public String title; public String userId; }

    @Data
    public static class UpdateSessionReq { public String title; public Boolean isFavorite; }

    @Data
    public static class CreateMessageReq {
        @NotNull public String sender; // user|assistant|system|tool
        @NotBlank public String content;
        public Object context;
        public boolean generate; // if true and sender=user, run RAG + generate assistant reply
        public String userId; // who owns the session (for access)
    }

    @Data
    public static class KnowledgeUpsertReq {
        @NotBlank public String content;
        public String source;
        public Object metadata;
        public String id; // optional UUID; if omitted server will create
    }

    @Data
    public static class PagedResponse<T> {
        public List<T> items;
        public int page;
        public int size;
        public long total;
    }
}
