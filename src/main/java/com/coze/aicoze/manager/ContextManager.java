package com.coze.aicoze.manager;

import com.coze.aicoze.enums.AgentType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ContextManager {

    // 会话上下文存储
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();
    // 存储每个 sessionId + agentType 对应的 Coze chatId
    // Key格式: sessionId:agentType, Value: Coze chatId
    private final Map<String, String> cozeChatIds = new ConcurrentHashMap<>();

    /**
     * 会话上下文
     */
    private static class SessionContext {
        AgentType mode; // 当前模式
        StringBuffer conversationHistory = new StringBuffer();
        String agentQuestion = null;
        int semanticClarifyCount = 0;
        long lastActiveTime;
        AtomicBoolean skillTriggered = new AtomicBoolean(false);
        AtomicBoolean careerTriggered = new AtomicBoolean(false);
    }

    /**
     * 初始化或获取上下文
     */
    public SessionContext initOrGetContext(String sessionId) {
        return sessionContexts.computeIfAbsent(sessionId, id -> {
            SessionContext context = new SessionContext();
            context.mode = AgentType.CHAT; // 默认聊天模式
            context.lastActiveTime = System.currentTimeMillis();
            return context;
        });
    }

    /**
     * 设置模式
     */
    public void setMode(AgentType mode, String sessionId) {
        SessionContext context = initOrGetContext(sessionId);
        context.mode = mode;
//        context.conversationHistory.setLength(0); // 切换模式时清空历史
    }

    /**
     * 获取当前模式
     */
    public AgentType getMode(String sessionId) {
        SessionContext context = sessionContexts.get(sessionId);
        return context != null ? context.mode : AgentType.CHAT;
    }

    /**
     * 添加对话历史
     */
    public void addToHistory(String sessionId, String userMessage, String aiResponse) {
        SessionContext context = initOrGetContext(sessionId);
        context.conversationHistory.append("用户: ").append(userMessage).append("\n");
        context.conversationHistory.append("AI: ").append(aiResponse).append("\n");
        context.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取对话历史
     */
    public String getConversationHistory(String sessionId) {
        SessionContext context = sessionContexts.get(sessionId);
        return context != null ? context.conversationHistory.toString() : "";
    }

    /**
     * 清理过期会话
     */
    public void cleanExpiredSessions(long timeoutMillis) {
        long now = System.currentTimeMillis();
        sessionContexts.entrySet().removeIf(entry ->
                now - entry.getValue().lastActiveTime > timeoutMillis
        );
    }

    /**
     * 获取或创建 Coze chatId
     * @param sessionId 会话ID
     * @param agentType Agent类型
     * @return Coze chatId，如果不存在则返回null
     */
    public String getCozeChatId(String sessionId, AgentType agentType) {
        String key = buildChatIdKey(sessionId, agentType);
        return cozeChatIds.get(key);
    }

    /**
     * 保存 Coze chatId
     * @param sessionId 会话ID
     * @param agentType Agent类型
     * @param cozeChatId Coze chatId
     */
    public void saveCozeChatId(String sessionId, AgentType agentType, String cozeChatId) {
        if (cozeChatId != null && !cozeChatId.isEmpty()) {
            String key = buildChatIdKey(sessionId, agentType);
            cozeChatIds.put(key, cozeChatId);
        }
    }

    /**
     * 清空指定 sessionId 的所有 Coze chatId
     * @param sessionId 会话ID
     */
    public void clearCozeChatIds(String sessionId) {
        cozeChatIds.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + ":"));
    }

    public void clearCozeChatId(String sessionId, AgentType agentType) {
        cozeChatIds.remove(buildChatIdKey(sessionId, agentType));
    }

    public Boolean getSkillTriggered(String sessionId) {
        return initOrGetContext(sessionId).skillTriggered.get();
    }

    public void  setSkillTriggered(String sessionId, Boolean trigger) {
        initOrGetContext(sessionId).skillTriggered.set(trigger);
    }

    public Boolean getCareerTriggered(String sessionId) {
        return initOrGetContext(sessionId).careerTriggered.get();
    }

    public void  setCareerTriggered(String sessionId, Boolean trigger) {
        initOrGetContext(sessionId).careerTriggered.set(trigger);
    }

    public String getAgentQuestion(String sessionId) {
        return initOrGetContext(sessionId).agentQuestion;
    }

    public void setAgentQuestion(String sessionId, String question) {
        initOrGetContext(sessionId).agentQuestion = question;
    }

    public int getSemanticClarifyCount(String sessionId) {
        return initOrGetContext(sessionId).semanticClarifyCount;
    }

    public void incrementSemanticClarifyCount(String sessionId) {
        initOrGetContext(sessionId).semanticClarifyCount++;
    }

    public void resetSemanticClarifyCount(String sessionId) {
        initOrGetContext(sessionId).semanticClarifyCount = 0;
    }
    /**
     * 构建 chatId 存储的 key
     */
    private String buildChatIdKey(String sessionId, AgentType agentType) {
        return sessionId + ":" + agentType.name();
    }
}