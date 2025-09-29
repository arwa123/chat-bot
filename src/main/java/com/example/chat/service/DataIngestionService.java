package com.example.chat.service;

import com.example.chat.dto.DocumentDto.DocumentResponse;
import com.example.chat.ingestion.document.DocumentProcessor;
import com.example.chat.ingestion.factory.DocumentProcessorFactory;
import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.pipeline.DataIngestionPipeline;
import com.example.chat.ingestion.pipeline.PipelineException;
import com.example.chat.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.chat.dto.ChatModelDto.DataIngestionResponse;
import com.example.chat.dto.ChatModelDto.DataIngestionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service for document processing and ingestion operations
 */
@Service
public class DataIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);
    
    private final DocumentProcessorFactory documentProcessorFactory;
    private final DataIngestionPipeline pipeline;

    @Autowired
    RagService ragService;
    
    public DataIngestionService(
            DocumentProcessorFactory documentProcessorFactory,
            DataIngestionPipeline pipeline) {
            
        this.documentProcessorFactory = documentProcessorFactory;
        this.pipeline = pipeline;
    }
    

    /**
     * Process a file upload through the ingestion pipeline asynchronously
     * 
     * @param file The uploaded file
     * @param metadata Additional metadata for the document
     * @return DocumentResponse with operation status and document ID
     */
    public DocumentResponse processFileAsync(MultipartFile file, Map<String, Object> metadata) {
        logger.info("Processing file upload asynchronously: {}", file.getOriginalFilename());
            try {
                String contentType = file.getContentType();
                DocumentProcessor processor = documentProcessorFactory.getProcessor();
                Document document = processor.processFile(file, metadata);
                CompletableFuture<List<UUID>> future = pipeline.processDataAsync(document);
                future.exceptionally(ex -> {
                    logger.error("Async processing failed for document {}: {}", document.id(), ex.getMessage(), ex);
                    return null;
                });
                return DocumentResponse.success(
                        document.id(),
                        "Successfully started processing the document"
                );
            } catch (Exception e) {
                throw new CompletionException(e);
            }
    }


    public DataIngestionResponse insertData(DataIngestionRequest request) {
        try {
            logger.info("Upserting knowledge chunk");

            UUID id = request.id() == null || request.id().isBlank() ?
                    UUID.randomUUID() : UUID.fromString(request.id());

            String metadataJson = request.metadata() == null ? null : request.metadata().toString();

            ragService.insertDataChunk(id, request.source(), request.content(), metadataJson);

            return DataIngestionResponse.success(id, "Knowledge chunk successfully stored");

        } catch (Exception e) {
            logger.error("Error upserting knowledge chunk", e);

            return DataIngestionResponse.error("Error: " + e.getMessage());
        }
    }
}