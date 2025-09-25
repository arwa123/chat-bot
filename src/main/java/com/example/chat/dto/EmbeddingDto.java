package com.example.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for embedding and knowledge base operations
 * Using Java Records for immutable data transfer objects
 */
public class EmbeddingDto {

    public record EmbeddingTensorInput(
            String name,
            List<Integer> shape,
            String datatype,
            List<String> data
    ) {}

    public record EmbeddingRequest(
            List<EmbeddingTensorInput> inputs
    ) {
        public static EmbeddingRequest forSingleText(String text) {
            return new EmbeddingRequest(List.of(
                    new EmbeddingTensorInput(
                            "input",
                            List.of(1),
                            "BYTES",
                            List.of(text)
                    )
            ));
        }

        public static EmbeddingRequest forMultipleTexts(List<String> texts) {
            return new EmbeddingRequest(List.of(
                    new EmbeddingTensorInput(
                            "input",
                            List.of(texts.size()),
                            "BYTES",
                            texts
                    )
            ));
        }
    }

    /**
     * Converter to handle JSON deserialization for embedding data that might be nested differently
     */
    public static class EmbeddingDataConverter extends StdConverter<Object, List<List<Double>>> {
        @Override
        public List<List<Double>> convert(Object value) {
            if (value == null) {
                return Collections.emptyList();
            }
            
            if (value instanceof List<?>) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    return Collections.emptyList();
                }
                
                if (list.get(0) instanceof List) {
                    return (List<List<Double>>) value;
                }
                
                if (list.get(0) instanceof Number) {
                    List<Double> doubles = new ArrayList<>();
                    for (Object obj : list) {
                        if (obj instanceof Number) {
                            doubles.add(((Number) obj).doubleValue());
                        }
                    }
                    return Collections.singletonList(doubles);
                }
            }
            
            if (value instanceof Number) {
                List<Double> singleValue = Collections.singletonList(((Number) value).doubleValue());
                return Collections.singletonList(singleValue);
            }
            
            return Collections.emptyList();
        }
    }

    public record EmbeddingTensorOutput(
            String name,
            List<Integer> shape,
            String datatype,
            @JsonDeserialize(converter = EmbeddingDataConverter.class)
            Object data
    ) {

        public List<List<Double>> getEmbeddingData() {
            if (data instanceof List) {
                return (List<List<Double>>) data;
            }
            return Collections.emptyList();
        }
    }

    public record EmbeddingResponse(
            String model_name,
            String model_version,
            List<EmbeddingTensorOutput> outputs
    ) {
        public List<List<Double>> getEmbeddings() {
            if (outputs == null || outputs.isEmpty() || outputs.get(0) == null) {
                return Collections.emptyList();
            }

            EmbeddingTensorOutput output = outputs.get(0);
            return output.getEmbeddingData();
        }

        public List<Double> getFirstEmbedding() {
            List<List<Double>> embeddings = getEmbeddings();
            if (embeddings == null || embeddings.isEmpty()) {
                return Collections.emptyList();
            }
            return embeddings.get(0);
        }
    }

    public record EmbeddingError(
            String error,
            @JsonProperty("error_type")
            String errorType,
            String message
    ) {}
}