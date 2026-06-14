package com.coze.aicoze.service;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.openapi.client.chat.model.ChatUsage;

import java.util.function.Consumer;

public interface ChatService {
    String startNewChat();

    ChatPayload sendMessage(String sessionId, String userMessage);

    void streamChat(String sessionId, String userMessage, Consumer<String> onContent, Consumer<ChatUsage> onComplete);
}
