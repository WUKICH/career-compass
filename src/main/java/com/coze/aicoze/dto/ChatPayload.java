package com.coze.aicoze.dto;

import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.dto.experience.SkillTestQuestionDto;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.ConsensusResultVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.PreliminaryRecommendationVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatPayload {
    /** 当前流程阶段（WorkflowEngine） */
    private String phase;
    /** 当前阶段允许的 Command 名称列表 */
    private List<String> allowedCommands;
    /** 主聊阶段：IntentRouter 建议可开始技能测评（双通道之一） */
    private Boolean suggestStartSkill;
    /** 技能测评题目是否已预生成就绪 */
    private Boolean skillQuestionsPrepared;
    private String sessionId;
    private String content;
    /** 对话中 AI 返回的快捷选项，前端在「快捷选项」面板展示 */
    private List<String> presetOptions;
    private boolean skillTestReady;
    private boolean careerTestReady;
    private String token;  // JWT Token
    private SkillAnalysisReportVO skillReport; // 技能测试结果报告（结构化）
    private HollandAnalysisVO hollandReport;   // 兴趣测试结果报告（结构化）
    private CareerAssessmentVO careerAssessmentReport; //总结报告
    private PreliminaryRecommendationVO preliminaryRecommendation;
    private ConsensusResultVO consensusResult;
    /**
     * 技能测试题目（结构化）
     */
    private SkillTestQuestionDto skillTestQuestions;
    /**
     * 霍兰德职业兴趣测试题目（结构化）
     */
    private CareerTestQuestionDto careerTestQuestions;
}



