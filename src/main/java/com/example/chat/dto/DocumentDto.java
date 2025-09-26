package com.example.chat.dto;

import java.util.UUID;


public class DocumentDto {



    public record DocumentResponse(
            UUID documentId,
            int chunkCount,
            String message,
            boolean success
    ) {
        public static DocumentResponse success(UUID documentId, int chunkCount, String message) {
            return new DocumentResponse(documentId, chunkCount, message, true);
        }

        public static DocumentResponse error(String message) {
            return new DocumentResponse(null, 0, message, false);
        }
    }
}