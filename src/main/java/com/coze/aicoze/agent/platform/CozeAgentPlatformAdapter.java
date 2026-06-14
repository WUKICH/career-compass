package com.coze.aicoze.agent.platform;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.full.flow.FlowMarkerApplier;
import com.coze.aicoze.service.PlatformOutboundRateLimiter;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.chat.model.ChatPoll;
import com.coze.openapi.client.chat.model.ChatStatus;
import com.coze.openapi.client.chat.model.ChatUsage;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.service.CozeAPI;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CozeAgentPlatformAdapter implements AgentPlatformAdapter {
    @Value("${coze.retry.conversation-occupied.enabled:true}")
    private boolean occupiedRetryEnabled;
    @Value("${coze.retry.conversation-occupied.max-attempts:3}")
    private int occupiedRetryMaxAttempts;
    @Value("${coze.retry.conversation-occupied.base-delay-ms:300}")
    private long occupiedRetryBaseDelayMs;

    @Resource
    private CozeAPI cozeClient;
    @Resource
    private FlowMarkerApplier flowMarkerApplier;
    @Resource
    private PlatformOutboundRateLimiter platformOutboundRateLimiter;

    @Override
    public String provider() {
        return "coze";
    }

    @Override
    public PlatformChatResponse chat(String userMessage, String botId, String sessionId, String userId, String conversationId, AgentType agentType) {
        CreateChatReq chatReq = buildReq(userMessage, botId, userId, conversationId);
        int maxAttempts = Math.max(1, occupiedRetryMaxAttempts);
        long baseDelayMs = Math.max(50L, occupiedRetryBaseDelayMs);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                platformOutboundRateLimiter.acquireCozeOrThrow();
                ChatPoll resp = cozeClient.chat().createAndPoll(chatReq);
                String content = resolvePollMessagesContent(resp.getMessages(), sessionId);
                if (agentType == AgentType.NLP_START) {
                    content = CozeJsonExtractor.extractLastJsonObject(content);
                }
                if (resp.getChat().getStatus() == ChatStatus.COMPLETED) {
                    log.info("token usage: {}, BotID: {}", resp.getChat().getUsage().getTokenCount(), resp.getChat().getBotID());
                }
                return PlatformChatResponse.builder()
                        .content(content)
                        .conversationId(resp.getChat().getConversationID())
                        .usage(resp.getChat().getUsage())
                        .build();
            } catch (Exception e) {
                if (occupiedRetryEnabled && isConversationOccupied(e) && attempt < maxAttempts) {
                    long waitMs = baseDelayMs * attempt;
                    log.warn("Coze conversation occupied, retrying... sessionId={}, agentType={}, attempt={}/{}, waitMs={}",
                            sessionId, agentType, attempt, maxAttempts, waitMs);
                    sleepQuietly(waitMs);
                    continue;
                }
                log.error("Coze sync chat failed, sessionId={}, agentType={}", sessionId, agentType, e);
                throw new BusinessException(BizErrorCodes.UNKNOWN_SYSTEM, "智能体调用失败，请稍后再试。");
            }
        }
        throw new BusinessException(BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT, "会话繁忙，请稍后再试。");
    }

    @Override
    public void streamChat(String userMessage, String botId, String sessionId, String userId, String conversationId, AgentType agentType, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        CreateChatReq chatReq = buildReq(userMessage, botId, userId, conversationId);
        int maxAttempts = Math.max(1, occupiedRetryMaxAttempts);
        long baseDelayMs = Math.max(50L, occupiedRetryBaseDelayMs);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                platformOutboundRateLimiter.acquireCozeOrThrow();
                Flowable<ChatEvent> resp = cozeClient.chat().stream(chatReq);
                resp.blockingForEach(event -> {
                    if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                        String cleaned = flowMarkerApplier.apply(event.getMessage().getContent(), sessionId);
                        if (cleaned != null && !cleaned.isEmpty()) {
                            onContent.accept(cleaned);
                        }
                    }
                    if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                        onComplete.accept(event.getChat().getUsage());
                    }
                });
                return;
            } catch (Exception e) {
                if (occupiedRetryEnabled && isConversationOccupied(e) && attempt < maxAttempts) {
                    long waitMs = baseDelayMs * attempt;
                    log.warn("Coze stream conversation occupied, retrying... sessionId={}, agentType={}, attempt={}/{}, waitMs={}",
                            sessionId, agentType, attempt, maxAttempts, waitMs);
                    sleepQuietly(waitMs);
                    continue;
                }
                log.error("Coze stream chat failed, sessionId={}, agentType={}", sessionId, agentType, e);
                throw new BusinessException(BizErrorCodes.UNKNOWN_SYSTEM, "智能体调用失败，请稍后再试。");
            }
        }
        throw new BusinessException(BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT, "会话繁忙，请稍后再试。");
    }

    private CreateChatReq buildReq(String userMessage, String botId, String userId, String conversationId) {
        CreateChatReq.CreateChatReqBuilder builder = CreateChatReq.builder()
                .botID(botId)
                .userID(userId)
                .messages(Collections.singletonList(Message.buildUserQuestionText(userMessage)));
        if (conversationId != null && !conversationId.isEmpty()) {
            try {
                builder.conversationID(conversationId);
            } catch (Exception ignore) {
                // SDK compatibility fallback.
            }
        }
        return builder.build();
    }

    private String resolvePollMessagesContent(List<Message> messages, String sessionId) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = 0; i < messages.size(); i++) {
            String content = messages.get(i).getContent();
            if (content != null && content.contains("generate_answer_finish")) {
                if (i > 0) {
                    String prev = messages.get(i - 1).getContent();
                    return flowMarkerApplier.apply(prev != null ? prev : "", sessionId).trim();
                }
                break;
            }
        }
        return messages.stream()
                .map(Message::getContent)
                .filter(c -> !CozeJsonExtractor.isJsonContent(c))
                .map(content -> flowMarkerApplier.apply(content, sessionId))
                .collect(Collectors.joining())
                .trim();
    }

    private boolean isConversationOccupied(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("4016")
                || msg.contains("Conversation occupied")
                || msg.contains("Another chat is currently active");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
