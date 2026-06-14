package com.coze.aicoze.workflow.support;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.agent.bots.ConsensusAgent;
import com.coze.aicoze.agent.bots.RecommendationAgent;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.ConsensusResultVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.PreliminaryRecommendationVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import com.coze.aicoze.workflow.composer.ReportComposer;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 霍兰德完成后：初步推荐 → 共识对话 → 结构化综合报告。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCareerWorkflowService {

    private final WorkflowEngine workflowEngine;
    private final WorkflowLifecycleService workflowLifecycleService;
    private final RecommendationAgent recommendationAgent;
    private final ConsensusAgent consensusAgent;
    private final PromptFactory promptFactory;
    private final ReportComposer reportComposer;

    /**
     * 霍兰德结果落库后调用：生成初步推荐并进入共识阶段。
     */
    public ChatPayload startPostCareerFlow(String sessionId, AssessmentStore.SessionAssessment session,
                                           HollandAnalysisVO hollandReport, String hollandJson) {
        workflowLifecycleService.onCareerComplete(sessionId, hollandJson);
        workflowEngine.enterPreliminaryRec(sessionId);

        String skillJson = workflowEngine.getState(sessionId).getSkillSummaryJson();
        String prompt = promptFactory.buildPreliminaryRecommendationPrompt(
                session.getTargetJob(), session.getSkill(), session.getExpectedCity(),
                skillJson, hollandJson);
        ChatResult recResult = recommendationAgent.doChat(prompt, sessionId);
        String recRaw = recResult.getContent();
        String recJson = CozeJsonExtractor.extractLastJsonObject(recRaw);
        if (StrUtil.isBlank(recJson)) {
            recJson = recRaw;
        }
        PreliminaryRecommendationVO preliminary = JsonParseUtil.toBeanQuiet(recJson, PreliminaryRecommendationVO.class);
        if (preliminary == null || preliminary.getRecommendedRoles() == null || preliminary.getRecommendedRoles().isEmpty()) {
            log.error("初步推荐 JSON 解析失败, sessionId={}", sessionId);
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE,
                    "初步岗位推荐生成失败，请稍后重试。");
        }
        if (StrUtil.isBlank(preliminary.getStatus())) {
            preliminary.setStatus("ready");
        }
        workflowEngine.storePreliminaryRec(sessionId, recJson);
        workflowEngine.startConsensus(sessionId);

        String display = formatPreliminaryDisplay(preliminary);
        return workflowLifecycleService.enrich(ChatPayload.builder()
                .sessionId(sessionId)
                .content(display)
                .hollandReport(hollandReport)
                .preliminaryRecommendation(preliminary)
                .skillTestReady(false)
                .careerTestReady(false)
                .build(), sessionId);
    }

    /**
     * 共识阶段用户消息。
     */
    public ChatPayload handleConsensusMessage(String sessionId, String userMessage,
                                              AssessmentStore.SessionAssessment session) {
        workflowEngine.assertCommandAllowed(sessionId,
                com.coze.aicoze.workflow.model.FlowCommand.CONSENSUS_MESSAGE);
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        if (state.getPhase() != FlowPhase.CONSENSUS_ACTIVE) {
            throw new BusinessException(BizErrorCodes.INVALID_WORKFLOW_PHASE, "当前不在共识对话阶段");
        }

        String wrapped = promptFactory.buildConsensusUserMessage(
                state.getPreliminaryRecJson(), state.getSkillSummaryJson(),
                state.getCareerSummaryJson(), userMessage);
        ChatResult chatResult = consensusAgent.doChat(wrapped, sessionId);
        String raw = chatResult.getContent();
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        if (StrUtil.isBlank(json)) {
            json = raw;
        }
        ConsensusResultVO consensus = JsonParseUtil.toBeanQuiet(json, ConsensusResultVO.class);
        if (consensus == null) {
            log.warn("共识回复非 JSON，按纯文本展示, sessionId={}", sessionId);
            return workflowLifecycleService.enrich(ChatPayload.builder()
                    .sessionId(sessionId)
                    .content(StrUtil.blankToDefault(raw, "请继续说明您的想法。"))
                    .presetOptions(chatResult.getPresetOptions())
                    .build(), sessionId);
        }

        if ("complete".equalsIgnoreCase(StrUtil.blankToDefault(consensus.getStatus(), ""))) {
            workflowEngine.storeConsensusResult(sessionId, json);
            CareerAssessmentVO report = reportComposer.composeFromStructured(sessionId, session, consensus);
            HollandAnalysisVO holland = parseHollandQuiet(state.getCareerSummaryJson(), session);
            return workflowLifecycleService.enrich(ChatPayload.builder()
                    .sessionId(sessionId)
                    .content("共识已完成，已为您生成综合评价报告，请点击下方按钮查看。")
                    .hollandReport(holland)
                    .careerAssessmentReport(report)
                    .skillTestReady(false)
                    .careerTestReady(false)
                    .build(), sessionId);
        }

        String reply = StrUtil.blankToDefault(consensus.getReply(), raw);
        return workflowLifecycleService.enrich(ChatPayload.builder()
                .sessionId(sessionId)
                .content(reply)
                .consensusResult(consensus)
                .presetOptions(chatResult.getPresetOptions())
                .build(), sessionId);
    }

    public static String formatPreliminaryDisplay(PreliminaryRecommendationVO prelim) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(prelim.getIntro())) {
            sb.append(prelim.getIntro().trim()).append("\n\n");
        }
        if (prelim.getRecommendedRoles() != null) {
            int i = 1;
            for (PreliminaryRecommendationVO.RecommendedRole role : prelim.getRecommendedRoles()) {
                if (role == null) {
                    continue;
                }
                sb.append(i++).append(". **").append(StrUtil.blankToDefault(role.getTitle(), "岗位"))
                        .append("**：").append(StrUtil.blankToDefault(role.getReason(), "")).append("\n");
            }
            sb.append("\n");
        }
        if (StrUtil.isNotBlank(prelim.getConfirmQuestion())) {
            sb.append(prelim.getConfirmQuestion().trim());
        } else {
            sb.append("以上推荐是否符合您的预期？请直接回复您的想法（如：符合 / 不符合 / 还行）。");
        }
        return sb.toString().trim();
    }

    private HollandAnalysisVO parseHollandQuiet(String hollandJson, AssessmentStore.SessionAssessment session) {
        HollandAnalysisVO holland = JsonParseUtil.toBeanQuiet(hollandJson, HollandAnalysisVO.class);
        if (holland != null) {
            holland.setName(session.getName());
            holland.setTargetJob(session.getTargetJob());
        }
        return holland;
    }
}
