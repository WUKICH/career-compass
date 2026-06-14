package com.coze.aicoze.agent;

import com.coze.aicoze.agent.platform.AgentPlatformAdapter;
import com.coze.aicoze.agent.platform.AgentPlatformSelector;
import com.coze.aicoze.agent.platform.PlatformChatResponse;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.entity.ChatMessage;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.enums.ChatRole;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.mapper.ChatMessageMapper;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.openapi.client.chat.model.ChatUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;


@Slf4j
public abstract class BaseChatAgent {

    @Resource
    private AgentPlatformSelector platformSelector;
    @Resource
    private ContextManager contextManager;
    @Resource
    private AssessmentStore assessmentStore;
    @Resource
    private Executor assessmentExecutor;
    @Resource
    private ChatMessageMapper chatMessageMapper;

    protected abstract AgentType getAgentType();
    protected abstract String getConfiguredPlatform();

    private AgentPlatformAdapter resolvePlatformAdapter() {
        return platformSelector.resolve(getConfiguredPlatform());
    }

    protected ChatResult chat(String userMessage, String botId, String sessionId) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.init(sessionId);
        Integer userId = sessionAssessment.getUserId();
        String providerUserId = (userId != null) ? String.valueOf(userId) : "2";
        String existingConversationId = contextManager.getCozeChatId(sessionId, getAgentType());
        try {
            PlatformChatResponse response = resolvePlatformAdapter().chat(
                    userMessage, botId, sessionId, providerUserId, existingConversationId, getAgentType());

            if (userId != null) {
                Integer inputCount = response.getUsage() == null ? 0 : response.getUsage().getInputCount();
                saveChatAsync(sessionId, userId, userMessage, "USER", inputCount);
            }

            String conversationId = response.getConversationId();
            if (conversationId != null && !conversationId.isEmpty()) {
                contextManager.saveCozeChatId(sessionId, getAgentType(), conversationId);
            }

            String collect = response.getContent();
            if (getAgentType() == AgentType.NLP_START) {
                collect = CozeJsonExtractor.extractLastJsonObject(collect);
            }
            if (getAgentType() == AgentType.CHAT && contextManager.getAgentQuestion(sessionId) == null
                    && contextManager.getMode(sessionId) != AgentType.CAREER_TEST
                    && !collect.contains("HOLLAND_TEST")
                    && !collect.contains("MBTI_TEST")) {
                contextManager.setAgentQuestion(sessionId, collect);
            }

            if (userId != null) {
                Integer outputCount = response.getUsage() == null ? 0 : response.getUsage().getOutputCount();
                saveChatAsync(sessionId, userId, collect, getAgentType().toString(), outputCount);
            }
            return ChatResult.builder()
                    .content(collect)
                    .skillTriggered(contextManager.getSkillTriggered(sessionId))
                    .careerTriggered(contextManager.getCareerTriggered(sessionId))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AI service call failed: " + e.getMessage(), e);
        }
    }

    protected String createPollAndExtractAssistantText(String userMessage, String botId, String sessionId) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        Integer userId = sessionAssessment.getUserId();
        String providerUserId = (userId != null) ? String.valueOf(userId) : "2";
        String existingConversationId = contextManager.getCozeChatId(sessionId, getAgentType());
        PlatformChatResponse response = resolvePlatformAdapter().chat(
                userMessage, botId, sessionId, providerUserId, existingConversationId, getAgentType());
        if (userId != null) {
            Integer inputCount = response.getUsage() == null ? 0 : response.getUsage().getInputCount();
            saveChatAsync(sessionId, userId, userMessage, "USER", inputCount);
        }
        if (response.getConversationId() != null && !response.getConversationId().isEmpty()) {
            contextManager.saveCozeChatId(sessionId, getAgentType(), response.getConversationId());
        }
        String collect = response.getContent();
        if (userId != null) {
            Integer outputCount = response.getUsage() == null ? 0 : response.getUsage().getOutputCount();
            saveChatAsync(sessionId, userId, collect, getAgentType().toString(), outputCount);
        }
        return collect;
    }

    protected void chatStream(String userMessage, String botId, String sessionId,
                              Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.init(sessionId);
        Integer userId = sessionAssessment.getUserId();
        String providerUserId = (userId != null) ? String.valueOf(userId) : "2";
        String existingConversationId = contextManager.getCozeChatId(sessionId, getAgentType());
        resolvePlatformAdapter().streamChat(userMessage, botId, sessionId, providerUserId, existingConversationId, getAgentType(), onContent, onComplete);
    }

    private void saveChatAsync(String sessionId, Integer participantId, String message, String type, Integer tokenUsage) {
        CompletableFuture.runAsync(() -> {
            try {
                ChatMessage row = new ChatMessage();
                row.setSessionId(sessionId);
                row.setParticipantId(participantId);
                row.setMessage(message);
                row.setTokenUsage(tokenUsage == null ? 0 : tokenUsage);
                if ("USER".equals(type)) {
                    row.setRole(ChatRole.USER.name());
                    row.setAgentType(null);
                } else {
                    row.setRole(ChatRole.ASSISTANT.name());
                    row.setAgentType(type);
                }
                chatMessageMapper.insert(row);
            } catch (Exception e) {
                log.error("Persist chat error, sessionId: {}, participantId: {}, type: {}", sessionId, participantId, type, e);
            }
        }, assessmentExecutor);
    }

    public abstract ChatResult doChat(String userMessage, String sessionId);

    public abstract void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete);
}
