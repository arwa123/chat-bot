package com.example.ragchat.service;

import com.example.ragchat.model.ChatMessage;
import com.example.ragchat.model.ChatSession;
import com.example.ragchat.model.Sender;
import com.example.ragchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatMessageRepository messageRepo;

    public ChatMessage addMessage(ChatSession session, Sender sender, String content, String contextJson) {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(session)
                .sender(sender)
                .content(content)
                .context(contextJson)
                .createdAt(OffsetDateTime.now())
                .build();
        return messageRepo.save(msg);
    }

    public Page<ChatMessage> listMessages(ChatSession session, int page, int size) {
        return messageRepo.findBySessionOrderByCreatedAtAsc(session, PageRequest.of(page, size));
    }
}
