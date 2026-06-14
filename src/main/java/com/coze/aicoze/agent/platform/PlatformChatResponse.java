package com.coze.aicoze.agent.platform;

import com.coze.openapi.client.chat.model.ChatUsage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformChatResponse {
    private String content;
    private String conversationId;
    private ChatUsage usage;
}
