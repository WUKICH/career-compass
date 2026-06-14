package com.coze.aicoze.workflow.invoker;

import com.coze.aicoze.agent.platform.AgentPlatformAdapter;
import com.coze.aicoze.agent.platform.AgentPlatformSelector;
import com.coze.aicoze.agent.platform.PlatformChatResponse;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.workflow.model.FlowPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Workflow 路径下的 Coze 调用收敛：stateless Router、阶段切换重置 conversation。
 */
@Component
@RequiredArgsConstructor
public class CozeBotInvoker {

    private final AgentPlatformSelector platformSelector;
    private final ContextManager contextManager;
    private final AssessmentStore assessmentStore;

    public PlatformChatResponse invoke(String userMessage,
                                       String botId,
                                       String sessionId,
                                       AgentType agentType,
                                       String platform) {
        return invoke(userMessage, botId, sessionId, agentType, platform, false);
    }

    public PlatformChatResponse invokeStateless(String userMessage,
                                                String botId,
                                                String sessionId,
                                                AgentType agentType,
                                                String platform) {
        return invoke(userMessage, botId, sessionId, agentType, platform, true);
    }

    public void resetConversationIfNeeded(String sessionId, AgentType agentType, FlowPhase enteringPhase) {
        if (ConversationPolicy.resetConversationOnEnter(enteringPhase)) {
            contextManager.clearCozeChatId(sessionId, agentType);
        }
    }

    private PlatformChatResponse invoke(String userMessage,
                                        String botId,
                                        String sessionId,
                                        AgentType agentType,
                                        String platform,
                                        boolean forceStateless) {
        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
        Integer userId = session.getUserId();
        String providerUserId = userId != null ? String.valueOf(userId) : "2";

        boolean stateless = forceStateless || ConversationPolicy.isStateless(agentType);
        String conversationId = stateless ? null : contextManager.getCozeChatId(sessionId, agentType);

        AgentPlatformAdapter adapter = platformSelector.resolve(platform);
        PlatformChatResponse response = adapter.chat(
                userMessage, botId, sessionId, providerUserId, conversationId, agentType);

        if (!stateless && response.getConversationId() != null && !response.getConversationId().isEmpty()) {
            contextManager.saveCozeChatId(sessionId, agentType, response.getConversationId());
        }
        return response;
    }
}
