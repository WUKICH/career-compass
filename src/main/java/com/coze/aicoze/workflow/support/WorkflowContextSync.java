package com.coze.aicoze.workflow.support;

import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.aicoze.workflow.model.FlowPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将 WorkflowEngine.phase 同步到 ContextManager.mode，供 RouterManager 路由 Agent。
 */
@Component
@RequiredArgsConstructor
public class WorkflowContextSync {

    private final ContextManager contextManager;
    private final WorkflowProperties workflowProperties;

    public void syncForPhase(String sessionId, FlowPhase phase) {
        if (phase == null) {
            return;
        }
        switch (phase) {
            case COLLECT, CONFIRMED, MAIN_CHAT, REPORT_READY, CLOSED -> {
                contextManager.setMode(AgentType.CHAT, sessionId);
                contextManager.setSkillTriggered(sessionId, false);
                contextManager.setCareerTriggered(sessionId, false);
            }
            case SKILL_ACTIVE -> {
                contextManager.setMode(AgentType.SKILL_TEST, sessionId);
                contextManager.setSkillTriggered(sessionId, true);
                contextManager.setCareerTriggered(sessionId, false);
            }
            case SKILL_DONE -> {
                contextManager.setMode(AgentType.SKILL_END, sessionId);
                contextManager.setSkillTriggered(sessionId, false);
                contextManager.setCareerTriggered(sessionId, false);
            }
            case CAREER_ACTIVE -> {
                contextManager.setMode(AgentType.CAREER_TEST, sessionId);
                contextManager.setCareerTriggered(sessionId, true);
                contextManager.setSkillTriggered(sessionId, false);
            }
            case CAREER_DONE -> {
                contextManager.setMode(AgentType.CAREER_END, sessionId);
                contextManager.setCareerTriggered(sessionId, false);
                contextManager.setSkillTriggered(sessionId, false);
            }
            case PRELIMINARY_REC -> contextManager.setMode(AgentType.RECOMMENDATION, sessionId);
            case CONSENSUS_ACTIVE -> contextManager.setMode(AgentType.CONSENSUS, sessionId);
            default -> { }
        }
    }
}
