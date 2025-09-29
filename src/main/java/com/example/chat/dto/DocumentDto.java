package com.example.chat.dto;

import java.util.UUID;


public class DocumentDto {



    public record DocumentResponse(
            UUID documentId,
            String message,
            boolean success
    ) {
        public static DocumentResponse success(UUID documentId, String message) {
            return new DocumentResponse(documentId, message, true);
        }

        public static DocumentResponse error(String message) {
            return new DocumentResponse(null , message, false);
        }
    }
}