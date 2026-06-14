package com.coze.aicoze.service.chat.pre;

/**
 * 主对话 {@code chat()} 进入路由 Agent 前的统一上下文。
 *
 * @param agentQuestionSnapshot 调用时刻的当前问题快照，与原先 {@code contextManager.getAgentQuestion(sessionId)} 一致
 */
public record ChatPreProcessContext(String sessionId, String userMessage, String agentQuestionSnapshot) {
}
