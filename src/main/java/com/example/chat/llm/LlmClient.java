package com.example.chat.llm;

/**
 * Interface for language model clients
 */
public interface LlmClient {
    /**
     * Generate text based on a prompt
     * 
     * @param prompt The prompt to generate from
     * @return The generated text
     */
    String generate(String prompt);
    
    /**
     * Get the model name used by this client
     * 
     * @return The model name
     */
    default String getModelName() {
        return "unknown";
    }
}
