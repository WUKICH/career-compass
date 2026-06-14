package com.coze.aicoze.agent.bots;

import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.enums.AgentType;
import com.coze.openapi.client.chat.model.ChatUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RecommendationAgent extends BaseChatAgent {

    @Value("${agent.platform.recommendation:${agent.platform.chat:${agent.platform.default:coze}}}")
    private String platform;

    @Value("${coze.recommendation-client.bot-id:${coze.chat-client.bot-id:}}")
    private String cozeBotId;

    @Value("${tencent.lke.recommendation-client.app-key:${tencent.lke.chat-client.app-key:}}")
    private String tencentBotId;

    @Override
    protected AgentType getAgentType() {
        return AgentType.RECOMMENDATION;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    public String resolveBotId() {
        return "coze".equalsIgnoreCase(platform) ? cozeBotId : tencentBotId;
    }

    public ChatResult doChat(String userMessage, String sessionId) {
        return chat(userMessage, resolveBotId(), sessionId);
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent,
                             Consumer<ChatUsage> onComplete) {
        chatStream(userMessage, resolveBotId(), sessionId, onContent, onComplete);
    }
}
