package com.coze.aicoze.workflow.model;

/**
 * 测评会话流程阶段。唯一状态源：仅 {@link com.coze.aicoze.workflow.engine.WorkflowEngine} 可变更。
 */
public enum FlowPhase {
    COLLECT,
    CONFIRMED,
    MAIN_CHAT,
    SKILL_ACTIVE,
    SKILL_DONE,
    CAREER_ACTIVE,
    CAREER_DONE,
    /** 生成初步岗位推荐（JSON） */
    PRELIMINARY_REC,
    /** 与用户多轮共识岗位偏好 */
    CONSENSUS_ACTIVE,
    REPORT_READY,
    CLOSED
}
