package com.coze.aicoze.service.chat;

/**
 * 对用户输入与当前 Agent 问题的语义相关性判断（NLP）。
 * <p>
 * 返回 {@code null} 表示通过；非空字符串为需直接展示给用户的拦截文案。
 * 供 {@link com.coze.aicoze.service.AssessmentService#semanticAnalysis} 与前置调用链共用，避免重复实现。
 */
public interface SemanticAnalysisService {

    /**
     * @param agentQuestion 当前轮问题快照；可与上下文中的 agentQuestion 同步
     */
    String analyze(String sessionId, String agentQuestion, String userAnswer);
}
