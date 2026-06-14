package com.coze.aicoze.agent.bots;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.enums.AgentType;
import com.coze.openapi.client.chat.model.ChatUsage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ResumeOptimizeAgent extends BaseChatAgent {

    @Value("${agent.platform.resume-optimize:${agent.platform.resume-parse:${agent.platform.default:coze}}}")
    private String platform;
    @Value("${coze.resume-optimize-client.bot-id:}")
    private String cozeBotId;
    @Value("${tencent.lke.resume-optimize-client.app-key:}")
    private String tencentBotId;

    @Override
    protected AgentType getAgentType() {
        return AgentType.RESUME_OPTIMIZE;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    public boolean isConfigured() {
        return StrUtil.isNotBlank(resolveBotId());
    }

    @Override
    public ChatResult doChat(String userMessage, String sessionId) {
        return chat(userMessage, resolveBotId(), sessionId);
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        chatStream(userMessage, resolveBotId(), sessionId, onContent, onComplete);
    }

    private String resolveBotId() {
        return "coze".equalsIgnoreCase(platform) ? cozeBotId : tencentBotId;
    }
}
