package com.example.ragchat.controller;

import com.example.ragchat.model.ChatSession;
import com.example.ragchat.service.SessionService;
import com.example.ragchat.dto.Dtos.CreateSessionReq;
import com.example.ragchat.dto.Dtos.UpdateSessionReq;
import com.example.ragchat.dto.Dtos.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<ChatSession> create(@RequestBody CreateSessionReq req) {
        System.out.println("blaahhhhh");
        if (req == null || req.userId == null || req.userId.isBlank())
            return ResponseEntity.badRequest().build();
        ChatSession s = sessionService.createSession(req.userId, req.title);
        return ResponseEntity.ok(s);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ChatSession>> list(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatSession> p = sessionService.listSessions(userId, page, size);
        PagedResponse<ChatSession> resp = new PagedResponse<>();
        resp.items = p.getContent();
        resp.page = page; resp.size = size; resp.total = p.getTotalElements();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatSession> get(@PathVariable UUID id) {
        return sessionService.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ChatSession> update(@PathVariable UUID id, @RequestBody UpdateSessionReq req) {
        ChatSession s = sessionService.update(id, req.title, req.isFavorite);
        return ResponseEntity.ok(s);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
