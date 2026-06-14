package com.coze.aicoze.agent.platform;

import com.coze.aicoze.enums.AgentType;
import com.coze.openapi.client.chat.model.ChatUsage;

import java.util.function.Consumer;

public interface AgentPlatformAdapter {

    String provider();

    PlatformChatResponse chat(String userMessage, String botId, String sessionId, String userId, String conversationId, AgentType agentType);

    void streamChat(String userMessage,
                    String botId,
                    String sessionId,
                    String userId,
                    String conversationId,
                    AgentType agentType,
                    Consumer<String> onContent,
                    Consumer<ChatUsage> onComplete);
}
