package com.coze.aicoze.workflow.invoker;

import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.workflow.model.FlowPhase;

/**
 * Coze 会话策略：Router 无会话；阶段切换时重置 conversation。
 */
public final class ConversationPolicy {

    private ConversationPolicy() {
    }

    /** stateless：不传 conversationId */
    public static boolean isStateless(AgentType agentType) {
        return agentType == AgentType.NLP_START || agentType == AgentType.ASSOCIATION_START;
    }

    /** 进入新 phase 时是否应新开 Coze 会话 */
    public static boolean resetConversationOnEnter(FlowPhase phase) {
        return phase == FlowPhase.SKILL_ACTIVE
                || phase == FlowPhase.CAREER_ACTIVE
                || phase == FlowPhase.CONSENSUS_ACTIVE
                || phase == FlowPhase.REPORT_READY;
    }
}
