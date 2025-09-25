package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.ExtractedContent;
import com.example.chat.ingestion.model.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Chunks text into fixed-size segments with optional overlap.
 * Simple but effective chunking strategy for most content types.
 */
@Component
public class FixedSizeTextChunker implements TextChunker {
    
    private static final Logger logger = LoggerFactory.getLogger(FixedSizeTextChunker.class);
    
    private final int defaultChunkSize;
    private final int defaultOverlapSize;
    private final ExecutorService chunkingExecutor;
    
    public FixedSizeTextChunker(
            @Value("${rag.chunking.default-chunk-size:1000}") int defaultChunkSize,
            @Value("${rag.chunking.default-overlap-size:100}") int defaultOverlapSize,
            @Value("${rag.pipeline.thread-pool-size:5}") int threadPoolSize) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlapSize = defaultOverlapSize;
        
        // Initialize thread pool for parallel chunking
        int poolSize = threadPoolSize > 0 ? threadPoolSize : 5;
        this.chunkingExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "chunking-worker-thread");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized fixed-size text chunker with chunk size {}, overlap {}, and thread pool size {}", 
                defaultChunkSize, defaultOverlapSize, poolSize);
    }
    
    @Override
    public String getType() {
        return "fixed-size";
    }
    
    @Override
    public List<TextChunk> chunk(ExtractedContent content) {
        return chunk(content, defaultChunkSize, defaultOverlapSize);
    }
    
    /**
     * Chunk text in parallel using CompletableFuture
     * 
     * @param content The extracted content to chunk
     * @return CompletableFuture that will resolve to a list of text chunks
     */
    public CompletableFuture<List<TextChunk>> chunkAsync(ExtractedContent content) {
        return chunkAsync(content, defaultChunkSize, defaultOverlapSize);
    }
    
    /**
     * Chunk text in parallel with specified chunk size and overlap
     * 
     * @param content The extracted content to chunk
     * @param maxChunkSize Maximum size of each chunk
     * @param overlapSize Size of overlap between chunks
     * @return CompletableFuture that will resolve to a list of text chunks
     */
    public CompletableFuture<List<TextChunk>> chunkAsync(ExtractedContent content, int maxChunkSize, int overlapSize) {
        return CompletableFuture.supplyAsync(() -> {
            return chunk(content, maxChunkSize, overlapSize);
        }, chunkingExecutor);
    }
    
    /**
     * Chunk text with specified chunk size and overlap
     * 
     * @param content The extracted content to chunk
     * @param maxChunkSize Maximum size of each chunk
     * @param overlapSize Size of overlap between chunks
     * @return List of text chunks
     */
    public List<TextChunk> chunk(ExtractedContent content, int maxChunkSize, int overlapSize) {
        return chunkWithStrategy(content, maxChunkSize, overlapSize, false);
    }
    
    /**
     * Internal method to chunk text with ability to use parallel processing for large texts
     * 
     * @param content The extracted content to chunk
     * @param maxChunkSize Maximum size of each chunk
     * @param overlapSize Size of overlap between chunks
     * @param useParallel Whether to use parallel processing for large texts
     * @return List of text chunks
     */
    private List<TextChunk> chunkWithStrategy(ExtractedContent content, int maxChunkSize, int overlapSize, boolean useParallel) {
        String text = content.getContent();
        
        if (text == null || text.isEmpty()) {
            logger.warn("Empty text provided for chunking");
            return Collections.emptyList();
        }
        
        // Validate chunk and overlap sizes
        if (maxChunkSize <= 0) {
            maxChunkSize = defaultChunkSize;
        }
        
        if (overlapSize < 0 || overlapSize >= maxChunkSize) {
            overlapSize = Math.min(defaultOverlapSize, maxChunkSize / 2);
        }
        
        logger.debug("Chunking text of length {} with chunk size {} and overlap {}", 
                text.length(), maxChunkSize, overlapSize);
        
        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();
        
        // If text is smaller than chunk size, return a single chunk
        if (textLength <= maxChunkSize) {
            chunks.add(createChunk(content, text, 0));
            return chunks;
        }
        
        // If the text is very large, we can use parallel processing
        boolean isLargeText = textLength > 100000 && useParallel; // Only process in parallel if text is large and flag is set
        
        if (isLargeText) {
            // Calculate the segments for parallel processing
            List<ChunkingTask> chunkingTasks = calculateChunkingTasks(text, maxChunkSize, overlapSize);
            
            // Process segments in parallel
            int finalMaxChunkSize = maxChunkSize;
            int finalOverlapSize = overlapSize;
            List<CompletableFuture<List<TextChunk>>> futures = chunkingTasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(() -> {
                        List<TextChunk> taskChunks = new ArrayList<>();
                        int position = task.startPosition;
                        int chunkIndex = task.startChunkIndex;
                        
                        while (position < task.endPosition) {
                            int end = Math.min(position + finalMaxChunkSize, task.endPosition);
                            
                            // Try to end at a sentence or paragraph boundary if possible
                            if (end < task.endPosition) {
                                int boundaryEnd = findBoundary(text, end);
                                if (boundaryEnd > position) {
                                    end = boundaryEnd;
                                }
                            }
                            
                            String chunkText = text.substring(position, end);
                            taskChunks.add(createChunk(content, chunkText, chunkIndex++));
                            
                            // Calculate next position with overlap
                            position = end - finalOverlapSize;
                            if (position <= 0 || position >= task.endPosition) {
                                break;
                            }
                        }
                        
                        return taskChunks;
                    }, chunkingExecutor))
                    .collect(Collectors.toList());
            
            // Combine results
            chunks = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .sorted(Comparator.comparingInt(TextChunk::getChunkIndex))
                    .collect(Collectors.toList());
        } else {
            // For smaller texts, process sequentially
            int position = 0;
            int chunkIndex = 0;
            
            while (position < textLength) {
                int end = Math.min(position + maxChunkSize, textLength);
                
                // Try to end at a sentence or paragraph boundary if possible
                if (end < textLength) {
                    int boundaryEnd = findBoundary(text, end);
                    if (boundaryEnd > position) {
                        end = boundaryEnd;
                    }
                }
                
                String chunkText = text.substring(position, end);
                chunks.add(createChunk(content, chunkText, chunkIndex++));
                
                // Calculate next position with overlap
                position = end - overlapSize;
                if (position <= 0 || position >= textLength) {
                    break;
                }
            }
        }
        
        logger.info("Created {} chunks from text of length {}", chunks.size(), textLength);
        return chunks;
    }
    
    /**
     * Find a suitable boundary (sentence or paragraph) to end a chunk
     * 
     * @param text The text being chunked
     * @param position Current position in text
     * @return Position of the next suitable boundary
     */
    private int findBoundary(String text, int position) {
        int textLength = text.length();
        
        // Search forward for paragraph break, period, or other sentence ending
        for (int i = position; i < Math.min(position + 100, textLength); i++) {
            char c = text.charAt(i);
            
            // Check for paragraph break
            if (c == '\n' && (i + 1 < textLength && text.charAt(i + 1) == '\n')) {
                return i + 2;
            }
            
            // Check for sentence ending (period, question mark, exclamation point)
            if ((c == '.' || c == '?' || c == '!') && 
                (i + 1 >= textLength || Character.isWhitespace(text.charAt(i + 1)))) {
                return i + 1;
            }
        }
        
        // If no good boundary found, search backward for a space
        for (int i = position; i > Math.max(position - 100, 0); i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        
        // If still no good boundary, just return the original position
        return position;
    }
    
    /**
     * Create a TextChunk from the extracted content and chunk text
     * 
     * @param content The original extracted content
     * @param chunkText The text for this chunk
     * @param chunkIndex The index of this chunk
     * @return A new TextChunk
     */
    /**
     * Helper class to represent a chunking task for parallel processing
     */
    private static class ChunkingTask {
        final int startPosition;
        final int endPosition;
        final int startChunkIndex;
        
        ChunkingTask(int startPosition, int endPosition, int startChunkIndex) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.startChunkIndex = startChunkIndex;
        }
    }
    
    /**
     * Calculate chunking tasks for parallel processing
     * 
     * @param text The text to chunk
     * @param maxChunkSize Maximum size of each chunk
     * @param overlapSize Size of overlap between chunks
     * @return List of chunking tasks
     */
    private List<ChunkingTask> calculateChunkingTasks(String text, int maxChunkSize, int overlapSize) {
        List<ChunkingTask> tasks = new ArrayList<>();
        int textLength = text.length();
        
        // Determine the number of parallel tasks based on text size
        int numTasks = Math.min(Runtime.getRuntime().availableProcessors(), 8); // Limit to avoid too many tasks
        int taskSize = textLength / numTasks;
        
        // Ensure task size is reasonable
        taskSize = Math.max(taskSize, maxChunkSize * 10);
        
        // Calculate task boundaries
        int position = 0;
        int chunkIndex = 0;
        
        while (position < textLength) {
            int endPosition = Math.min(position + taskSize, textLength);
            
            // Find a suitable boundary
            if (endPosition < textLength) {
                int boundaryEnd = findBoundary(text, endPosition);
                if (boundaryEnd > position) {
                    endPosition = boundaryEnd;
                }
            }
            
            // Calculate estimated number of chunks in this task
            int estimatedChunks = (endPosition - position) / (maxChunkSize - overlapSize) + 1;
            
            tasks.add(new ChunkingTask(position, endPosition, chunkIndex));
            
            position = endPosition;
            chunkIndex += estimatedChunks;
        }
        
        return tasks;
    }
    
    private TextChunk createChunk(ExtractedContent content, String chunkText, int chunkIndex) {
        // Create a minimal metadata map instead of copying everything from the original content
        // This significantly reduces memory usage when dealing with large documents
        Map<String, Object> metadata = new HashMap<>();
        
        // Add only essential metadata
        metadata.put("document_id", content.getId().toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_text_length", chunkText.length());
        
        // Only add source-related metadata if present and relevant
        if (content.getMetadata().containsKey("source_type")) {
            metadata.put("source_type", content.getMetadata().get("source_type"));
        }
        
        return TextChunk.builder()
                .id(UUID.randomUUID())
                .documentId(content.getId())
                .source(content.getSource())
                .content(chunkText)
                .metadata(metadata)
                .chunkIndex(chunkIndex)
                .build();
    }
}