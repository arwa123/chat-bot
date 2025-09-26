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
     * Process a file upload through the ingestion pipeline
     * 
     * @param file The uploaded file
     * @param metadata Additional metadata for the document
     * @return Response with operation status and document ID
     */
    public DocumentResponse processFile(MultipartFile file, Map<String, Object> metadata) {
        try {
            return processFileAsync(file, metadata).join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.error("Error processing file: {}", cause.getMessage(), cause);
            return DocumentResponse.error("Unexpected error: " + cause.getMessage());
        }
    }
    
    /**
     * Process a file upload through the ingestion pipeline asynchronously
     * 
     * @param file The uploaded file
     * @param metadata Additional metadata for the document
     * @return CompletableFuture that will resolve to a response with operation status and document ID
     */
    public CompletableFuture<DocumentResponse> processFileAsync(MultipartFile file, Map<String, Object> metadata) {
        logger.info("Processing file upload asynchronously: {}", file.getOriginalFilename());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String contentType = file.getContentType();
                DocumentProcessor processor = documentProcessorFactory.getProcessor(contentType);
                Document document = processor.processFile(file, metadata);
                List<UUID> chunkIds = pipeline.processDataAsync(document).join();
                
                return DocumentResponse.success(
                        document.id(),
                        chunkIds.size(),
                        "Successfully processed document and created " + chunkIds.size() + " chunks using parallel processing"
                );
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            logger.error("Error in async document processing: {}", cause.getMessage(), cause);
            if (cause instanceof IOException) {
                return DocumentResponse.error("Error processing file: " + cause.getMessage());
            } else if (cause instanceof PipelineException) {
                return DocumentResponse.error("Pipeline error: " + cause.getMessage());
            } else {
                return DocumentResponse.error("Unexpected error: " + cause.getMessage());
            }
        });
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