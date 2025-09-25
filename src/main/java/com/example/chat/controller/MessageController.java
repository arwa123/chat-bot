package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.model.ChatSession;
import com.example.chat.model.Sender;
import com.example.chat.rag.RagService;
import com.example.chat.service.MessageService;
import com.example.chat.service.SessionService;
import com.example.chat.dto.Dtos.CreateMessageReq;
import com.example.chat.dto.Dtos.PagedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final SessionService sessionService;
    private final MessageService messageService;
    private final RagService ragService;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> add(@PathVariable("sessionId") UUID sessionId, @RequestBody CreateMessageReq req) throws JsonProcessingException {
        ChatSession session = sessionService.get(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (req.userId != null && !req.userId.equals(session.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error","Forbidden","message","User mismatch"));
        }
        Sender sender = Sender.valueOf(req.sender);
        String contextJson = null;
        if (req.context != null) {
            contextJson = objectMapper.writeValueAsString(req.context);
        }
        ChatMessage userMsg = messageService.addMessage(session, sender, req.content, contextJson);

        if (req.generate && sender == Sender.user) {
            // RAG: retrieve top-k and generate assistant response
            var retrieved = ragService.retrieve(req.content);
            String answer = ragService.generateAugmentedAnswer(req.content, retrieved);
            // Store assistant message with context that includes retrieved chunks
            var ctx = Map.of("retrieved", retrieved.stream().map(r -> Map.of(
                    "id", r.id.toString(),
                    "source", r.source,
                    "score", r.score
            )).toList());
            ChatMessage assistant = messageService.addMessage(session, Sender.assistant, answer, new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(ctx).toString());
            return ResponseEntity.ok(Map.of("user", userMsg, "assistant", assistant));
        }
        return ResponseEntity.ok(userMsg);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ChatMessage>> list(@PathVariable UUID sessionId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        ChatSession session = sessionService.get(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        Page<ChatMessage> p = messageService.listMessages(session, page, size);
        PagedResponse<ChatMessage> resp = new PagedResponse<>();
        resp.items = p.getContent();
        resp.page = page; resp.size = size; resp.total = p.getTotalElements();
        return ResponseEntity.ok(resp);
    }
}
