package com.coze.aicoze.dto.full;

import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 完全版对话与测评响应，与体验版 {@link com.coze.aicoze.dto.ChatPayload} 分离，前端可单独适配。
 */
@Data
@Builder
public class FullChatPayload {

    private String sessionId;
    private String content;
    private List<String> presetOptions;
    private boolean skillTestReady;
    private boolean careerTestReady;
    private String token;

    private SkillAnalysisReportVO skillReport;
    private HollandAnalysisVO hollandReport;
    private CareerAssessmentVO careerAssessmentReport;

    /** 完全版技能测评题目（结构化，由 Bot 混排中的 JSON 解析） */
    private FullSkillTestQuestionDto fullSkillTestQuestions;
    /** 完全版霍兰德题目（结构化） */
    private FullCareerTestQuestionDto fullCareerTestQuestions;

    /**
     * 当前会话已出现的技能测评轮次（每收到一次 [SKILL_TEST] 递增，从 1 起）。
     */
    private Integer skillTestRound;

    /** Workflow 阶段（与体验版 {@code phase} 一致） */
    private String phase;
}
