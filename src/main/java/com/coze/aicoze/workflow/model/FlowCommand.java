package com.coze.aicoze.workflow.model;

/**
 * 前端/Controller 发起的流程命令。
 */
public enum FlowCommand {
    COLLECT_STEP,
    CONFIRM_PROFILE,
    MAIN_CHAT_MESSAGE,
    START_SKILL,
    SUBMIT_SKILL_ANSWER,
    SUBMIT_CAREER_ANSWER,
    /** 共识阶段用户发消息 */
    CONSENSUS_MESSAGE,
    GENERATE_REPORT,
    SUBMIT_FEEDBACK
}
