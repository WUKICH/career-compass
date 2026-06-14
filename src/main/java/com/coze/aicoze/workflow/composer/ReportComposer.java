package com.coze.aicoze.workflow.composer;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.infrastructure.knowledgegraph.service.KnowledgeGraphTalentSyncService;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.component.CareerResumeOptimizationEnricher;
import com.coze.aicoze.util.ExperienceQuestionParser;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.ConsensusResultVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.PreliminaryRecommendationVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import com.coze.aicoze.workflow.support.PostCareerWorkflowService;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.ReportScope;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CAREER_DONE 后固定生成综合报告，不依赖 [CHAT_END] marker。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportComposer {

    private final WorkflowEngine workflowEngine;
    private final AssessmentStore assessmentStore;
    private final AssessmentPersistence persistence;
    private final KnowledgeGraphTalentSyncService knowledgeGraphTalentSyncService;
    private final CareerResumeOptimizationEnricher careerResumeOptimizationEnricher;

    /** 从技能/霍兰德/共识 JSON 组装综合报告，不依赖主聊 Bot 自由文本。 */
    public CareerAssessmentVO composeFromStructured(String sessionId, AssessmentStore.SessionAssessment session,
                                                    ConsensusResultVO consensus) {
        log.info("ReportComposer composeFromStructured, sessionId={}", sessionId);
        CareerAssessmentVO report = new CareerAssessmentVO();
        report.setName(session.getName());
        report.setTargetJob(session.getTargetJob());
        report.setTargetCity(session.getExpectedCity());
        report.setExpectedSalary(session.getExpectedSalary());

        SessionWorkflowState state = workflowEngine.getState(sessionId);
        mapSkillAssessments(report, state.getSkillSummaryJson());
        mapHollandInterest(report, state.getCareerSummaryJson());

        if (consensus != null && consensus.getRecommendedPositions() != null
                && !consensus.getRecommendedPositions().isEmpty()) {
            report.setRecommendedPositions(consensus.getRecommendedPositions());
        } else {
            attachKgPositions(report, session.getUserId());
        }

        careerResumeOptimizationEnricher.enrichIfApplicable(session.getUserId(), sessionId, report);
        persistence.saveChatResult(session.getUserId(), sessionId,
                cn.hutool.json.JSONUtil.toJsonStr(report), "REPORT_COMPOSED");
        workflowEngine.markReportReady(sessionId);
        return report;
    }

    /**
     * 按进度组装阶段性报告（不写库、不推进 workflow）。
     */
    public CareerAssessmentVO composePartial(String sessionId, ReportScope scope) {
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "会话不存在");
        }
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        CareerAssessmentVO report = baseUserInfo(session);

        switch (scope) {
            case PROFILE -> { /* baseUserInfo only */ }
            case RESUME -> careerResumeOptimizationEnricher.enrichIfApplicable(
                    session.getUserId(), sessionId, report);
            case SKILL -> mapSkillAssessments(report, state.getSkillSummaryJson());
            case CAREER -> {
                mapSkillAssessments(report, state.getSkillSummaryJson());
                mapHollandInterest(report, state.getCareerSummaryJson());
            }
            case RECOMMENDATION -> {
                mapSkillAssessments(report, state.getSkillSummaryJson());
                mapHollandInterest(report, state.getCareerSummaryJson());
                attachRecommendationPositions(report, state);
            }
            case FULL -> {
                mapSkillAssessments(report, state.getSkillSummaryJson());
                mapHollandInterest(report, state.getCareerSummaryJson());
                ConsensusResultVO consensus = parseConsensus(state.getConsensusResultJson());
                if (consensus != null && consensus.getRecommendedPositions() != null
                        && !consensus.getRecommendedPositions().isEmpty()) {
                    report.setRecommendedPositions(consensus.getRecommendedPositions());
                } else {
                    attachRecommendationPositions(report, state);
                }
                careerResumeOptimizationEnricher.enrichIfApplicable(session.getUserId(), sessionId, report);
            }
            default -> throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "无效的报告范围");
        }
        log.info("ReportComposer composePartial scope={}, sessionId={}", scope, sessionId);
        return report;
    }

    /** 霍兰德兴趣测试完整报告（含 dimensionAnalysis、jobMatchAnalysis）。 */
    public HollandAnalysisVO composeHolland(String sessionId) {
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "会话不存在");
        }
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        String careerJson = state.getCareerSummaryJson();
        if (StrUtil.isBlank(careerJson)) {
            careerJson = session.getCareerResult();
        }
        if (StrUtil.isBlank(careerJson)) {
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "暂无霍兰德测评结果");
        }
        HollandAnalysisVO holland = JsonParseUtil.toBeanQuiet(careerJson, HollandAnalysisVO.class);
        if (holland == null) {
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "霍兰德报告解析失败");
        }
        holland.setName(session.getName());
        holland.setTargetJob(session.getTargetJob());
        if (holland.getTestTime() == null) {
            holland.setTestTime(java.time.LocalDateTime.now());
        }
        log.info("ReportComposer composeHolland sessionId={}", sessionId);
        return holland;
    }

    private CareerAssessmentVO baseUserInfo(AssessmentStore.SessionAssessment session) {
        CareerAssessmentVO report = new CareerAssessmentVO();
        report.setName(session.getName());
        report.setTargetJob(session.getTargetJob());
        report.setTargetCity(session.getExpectedCity());
        report.setExpectedSalary(session.getExpectedSalary());
        return report;
    }

    private ConsensusResultVO parseConsensus(String consensusJson) {
        if (StrUtil.isBlank(consensusJson)) {
            return null;
        }
        ConsensusResultVO consensus = JsonParseUtil.toBeanQuiet(consensusJson, ConsensusResultVO.class);
        if (consensus == null) {
            return null;
        }
        if (!"complete".equalsIgnoreCase(StrUtil.blankToDefault(consensus.getStatus(), ""))) {
            return null;
        }
        return consensus;
    }

    private void mapSkillAssessments(CareerAssessmentVO report, String skillSummaryJson) {
        if (StrUtil.isBlank(skillSummaryJson)) {
            return;
        }
        SkillAnalysisReportVO skill = JsonParseUtil.toBeanQuiet(skillSummaryJson, SkillAnalysisReportVO.class);
        if (skill == null || skill.getSkillAnalysis() == null) {
            return;
        }
        List<CareerAssessmentVO.SkillAssessment> list = new ArrayList<>();
        for (SkillAnalysisReportVO.SkillAnalysisVO sa : skill.getSkillAnalysis()) {
            if (sa == null) {
                continue;
            }
            CareerAssessmentVO.SkillAssessment item = new CareerAssessmentVO.SkillAssessment();
            item.setSkill(sa.getSkillName());
            item.setScore(sa.getScore());
            item.setPracticeScore(sa.getScore() != null ? String.valueOf(sa.getScore()) : null);
            list.add(item);
        }
        report.setSkillAssessments(list);
    }

    private void mapHollandInterest(CareerAssessmentVO report, String careerSummaryJson) {
        if (StrUtil.isBlank(careerSummaryJson)) {
            return;
        }
        HollandAnalysisVO holland = JsonParseUtil.toBeanQuiet(careerSummaryJson, HollandAnalysisVO.class);
        if (holland == null) {
            return;
        }
        CareerAssessmentVO.CareerInterestResult cir = new CareerAssessmentVO.CareerInterestResult();
        if (StrUtil.isNotBlank(holland.getHollandCode())) {
            cir.setCodes(Arrays.asList(holland.getHollandCode().trim().split("\\s+")));
        }
        cir.setDescription(holland.getTypeCombination());
        report.setCareerInterestResult(cir);
    }

    private void attachRecommendationPositions(CareerAssessmentVO report, SessionWorkflowState state) {
        ConsensusResultVO consensus = parseConsensus(state.getConsensusResultJson());
        if (consensus != null && consensus.getRecommendedPositions() != null
                && !consensus.getRecommendedPositions().isEmpty()) {
            report.setRecommendedPositions(consensus.getRecommendedPositions());
            return;
        }
        mapPreliminaryRecommendations(report, state.getPreliminaryRecJson());
    }

    private void mapPreliminaryRecommendations(CareerAssessmentVO report, String preliminaryRecJson) {
        if (StrUtil.isBlank(preliminaryRecJson)) {
            return;
        }
        PreliminaryRecommendationVO preliminary = JsonParseUtil.toBeanQuiet(
                preliminaryRecJson, PreliminaryRecommendationVO.class);
        if (preliminary == null || preliminary.getRecommendedRoles() == null
                || preliminary.getRecommendedRoles().isEmpty()) {
            return;
        }
        List<CareerAssessmentVO.PositionsItem> list = new ArrayList<>();
        for (PreliminaryRecommendationVO.RecommendedRole role : preliminary.getRecommendedRoles()) {
            if (role == null) {
                continue;
            }
            CareerAssessmentVO.PositionsItem item = new CareerAssessmentVO.PositionsItem();
            item.setTitle(role.getTitle());
            item.setCompIndustry(role.getReason());
            list.add(item);
        }
        if (!list.isEmpty()) {
            report.setRecommendedPositions(list);
        }
    }

    private void attachKgPositions(CareerAssessmentVO report, Integer userId) {
        List<CareerAssessmentVO.PositionsItem> kg = knowledgeGraphTalentSyncService.fetchRecommendedPositionsForReport(userId);
        if (kg != null && !kg.isEmpty()) {
            report.setRecommendedPositions(kg);
            return;
        }
        CareerAssessmentVO.PositionsItem placeholder = new CareerAssessmentVO.PositionsItem();
        placeholder.setTitle("推荐数据暂时获取不到");
        report.setRecommendedPositions(Collections.singletonList(placeholder));
    }

    public void enrichPayload(ChatPayload.ChatPayloadBuilder builder, SessionWorkflowState state, ChatPayload source) {
        if (state == null) {
            return;
        }
        builder.phase(state.getPhase().name());
        builder.allowedCommands(SessionWorkflowState.allowedCommandNames(state.getPhase()));
        builder.suggestStartSkill(state.isSuggestStartSkill());
        builder.skillQuestionsPrepared(state.isSkillQuestionsPrepared());
        attachCareerQuestionsForChat(builder, state);
        attachPreliminaryRecForChat(builder, state, source);
    }

    private void attachPreliminaryRecForChat(ChatPayload.ChatPayloadBuilder builder, SessionWorkflowState state,
                                             ChatPayload source) {
        if (state == null) {
            return;
        }
        FlowPhase phase = state.getPhase();
        if (phase != FlowPhase.CONSENSUS_ACTIVE && phase != FlowPhase.PRELIMINARY_REC) {
            return;
        }
        String json = state.getPreliminaryRecJson();
        if (StrUtil.isBlank(json)) {
            return;
        }
        PreliminaryRecommendationVO preliminary = JsonParseUtil.toBeanQuiet(json, PreliminaryRecommendationVO.class);
        if (preliminary == null) {
            return;
        }
        builder.preliminaryRecommendation(preliminary);
        if (source != null && source.getConsensusResult() != null) {
            return;
        }
        if (source != null && StrUtil.isNotBlank(source.getContent()) && phase == FlowPhase.CONSENSUS_ACTIVE) {
            return;
        }
        builder.content(PostCareerWorkflowService.formatPreliminaryDisplay(preliminary));
    }

    /** 技能测评完成后，将已预生成的霍兰德题目一并下发给前端（含 workflow/status 轮询）。 */
    private void attachCareerQuestionsForChat(ChatPayload.ChatPayloadBuilder builder, SessionWorkflowState state) {
        if (state == null) {
            return;
        }
        FlowPhase phase = state.getPhase();
        if (phase != FlowPhase.CAREER_ACTIVE && phase != FlowPhase.SKILL_DONE) {
            return;
        }
        String careerRaw = state.getCareerQuestionsJson();
        if (StrUtil.isBlank(careerRaw)) {
            AssessmentStore.SessionAssessment session = assessmentStore.get(state.getSessionId());
            if (session != null) {
                careerRaw = session.getCareerQuestion();
            }
        }
        if (StrUtil.isBlank(careerRaw)) {
            return;
        }
        CareerTestQuestionDto dto = ExperienceQuestionParser.parseCareerQuestions(careerRaw);
        if (dto != null) {
            builder.careerTestReady(true);
            builder.careerTestQuestions(dto);
        }
    }
}
