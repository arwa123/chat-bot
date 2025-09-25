package com.example.ragchat.rag;

import org.springframework.stereotype.Component;

@Component("stubLlmClient")
public class StubLlmClient implements LlmClient {
    @Override
    public String generate(String prompt) {
        return "Stub LLM response based on prompt:\n" + prompt.substring(0, Math.min(400, prompt.length()));
    }
}
