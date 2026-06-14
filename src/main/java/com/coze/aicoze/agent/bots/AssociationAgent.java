package com.coze.aicoze.agent.bots;

import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.enums.AgentType;
import com.coze.openapi.client.chat.model.ChatUsage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class AssociationAgent extends BaseChatAgent {

    @Value("${agent.platform.association:${agent.platform.default:coze}}")
    private String platform;
    @Value("${coze.association-client.bot-id:}")
    private String cozeBotId;
    @Value("${tencent.lke.association-client.app-key:}")
    private String tencentBotId;

    @Override
    protected AgentType getAgentType() {
        return AgentType.ASSOCIATION_START;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    @Override
    public ChatResult doChat(String currentQuestion, String sessionId) {
        return chat(currentQuestion, resolveBotId(), sessionId);
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        chatStream(userMessage, resolveBotId(), sessionId, onContent, onComplete);
    }

    private String resolveBotId() {
        return "coze".equalsIgnoreCase(platform) ? cozeBotId : tencentBotId;
    }
}
