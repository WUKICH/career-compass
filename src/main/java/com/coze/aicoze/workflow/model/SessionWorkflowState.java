package com.coze.aicoze.workflow.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SessionWorkflowState {

    private String sessionId;
    @Builder.Default
    private FlowPhase phase = FlowPhase.COLLECT;
    @Builder.Default
    private FlowVariant variant = FlowVariant.TRIAL;
    @Builder.Default
    private int mainChatTurnCount = 0;
    @Builder.Default
    private int skillRound = 0;
    private Integer userId;
    private String anchorProfileJson;
    private String skillQuestionsJson;
    private String careerQuestionsJson;
    private String skillSummaryJson;
    private String careerSummaryJson;
    private String preliminaryRecJson;
    private String consensusResultJson;
    /** 预生成题目是否就绪 */
    @Builder.Default
    private boolean skillQuestionsPrepared = false;
    @Builder.Default
    private boolean careerQuestionsPrepared = false;
  /** Router 建议展示「开始测评」 */
    @Builder.Default
    private boolean suggestStartSkill = false;
    private Instant updatedAt;

    public static List<String> allowedCommandNames(FlowPhase phase) {
        List<String> list = new ArrayList<>();
        switch (phase) {
            case COLLECT -> {
                list.add(FlowCommand.COLLECT_STEP.name());
                list.add(FlowCommand.CONFIRM_PROFILE.name());
            }
            case MAIN_CHAT -> {
                list.add(FlowCommand.MAIN_CHAT_MESSAGE.name());
                list.add(FlowCommand.START_SKILL.name());
            }
            case SKILL_ACTIVE -> list.add(FlowCommand.SUBMIT_SKILL_ANSWER.name());
            case CAREER_ACTIVE -> list.add(FlowCommand.SUBMIT_CAREER_ANSWER.name());
            case CONSENSUS_ACTIVE -> {
                list.add(FlowCommand.CONSENSUS_MESSAGE.name());
                list.add(FlowCommand.GENERATE_REPORT.name());
            }
            case REPORT_READY -> list.add(FlowCommand.SUBMIT_FEEDBACK.name());
            default -> { }
        }
        return list;
    }
}
