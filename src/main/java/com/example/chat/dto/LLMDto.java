package com.example.chat.dto;

import lombok.Builder;

import java.util.List;

public class LLMDto {


    public record Message(
            String role,
            String content
    ) {}


    @Builder
    public record AnthropicChatRequest(
            String model,
            int max_tokens,
            double temperature,
            List<Message> messages
    ){}

    public record AnthropicChatResponse(
            String id,
            String model,
            List<AnthropicChatChoice> choices,
            AnthropicChatUsage usage
    ) {
        public String getContent() {
            if (choices == null || choices.isEmpty() || choices.get(0) == null) {
                return null;
            }

            AnthropicChatChoice choice = choices.get(0);
            return choice.message() != null ? choice.message().content() : null;
        }
    }

    public record AnthropicChatChoice(
            int index,
            Message message,
            String finish_reason
    ) {}

    public record AnthropicChatUsage(
            int input_tokens,
            int output_tokens
    ) {}

    public record GeminiPart(
        String text
    ) {}

    public record GeminiContent(
        List<GeminiPart> parts
    ) {}

    public record GeminiGenerationConfig(
        int maxOutputTokens,
        double temperature,
        Double topP,
        List<String> stopSequences
    ) {
        public static GeminiGenerationConfig defaults() {
            return new GeminiGenerationConfig(512, 0.7, null, null);
        }
        
        public static GeminiGenerationConfig create(int maxTokens, double temperature) {
            return new GeminiGenerationConfig(maxTokens, temperature, null, null);
        }
    }

    public record GeminiRequest(
        List<GeminiContent> contents,
        GeminiGenerationConfig generationConfig,
        List<String> safetySettings
    ) {
        public static GeminiRequest create(String prompt) {
            return new GeminiRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
                GeminiGenerationConfig.defaults(),
                null
            );
        }

        public static GeminiRequest create(String prompt, int maxTokens, double temperature) {
            return new GeminiRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
                GeminiGenerationConfig.create(maxTokens, temperature),
                null
            );
        }
    }
    

    public record GeminiResponse(
        List<GeminiCandidate> candidates,
        String promptFeedback
    ) {
        public String getText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            
            GeminiCandidate candidate = candidates.get(0);
            if (candidate == null || candidate.content() == null) {
                return null;
            }
            
            List<GeminiPart> parts = candidate.content().parts();
            if (parts == null || parts.isEmpty()) {
                return null;
            }
            
            return parts.get(0).text();
        }
    }

    public record GeminiCandidate(
        GeminiContent content,
        String finishReason,
        int index,
        List<String> safetyRatings
    ) {}
}