package com.coze.aicoze.service.flow.handler.skill;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.bots.CareerTestAgent;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.infrastructure.knowledgegraph.service.KnowledgeGraphTalentSyncService;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.flow.SkillSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.service.flow.handler.SkillSubmitOutcomeHandler;
import com.coze.aicoze.util.AdjustedStoragePayload;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.ExperienceQuestionParser;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import com.coze.aicoze.workflow.invoker.CozeBotInvoker;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillCompleteOutcomeHandler implements SkillSubmitOutcomeHandler {

    private static final String CAREER_HANDOFF_MESSAGE = "职业兴趣测评即将开始，请做好准备！";

    private final AssessmentPersistence persistence;
    private final CareerTestAgent careerTestAgent;
    private final ContextManager contextManager;
    private final KnowledgeGraphTalentSyncService knowledgeGraphTalentSyncService;
    private final WorkflowLifecycleService workflowLifecycleService;
    private final WorkflowEngine workflowEngine;
    private final CozeBotInvoker cozeBotInvoker;

    @Override
    public TransitionOutcome outcome() {
        return TransitionOutcome.COMPLETE;
    }

    @Override
    public ChatPayload handle(SkillSubmitContext context) {
        persistence.saveSkill(context.userId(), context.sessionId(), context.skillResult(), "SKILL_RESULT");
        context.session().setSkillResult(context.skillResult());

        String jsonForSkillReport = CozeJsonExtractor.extractLastJsonObject(context.skillResult());
        if (StrUtil.isBlank(jsonForSkillReport)) {
            jsonForSkillReport = context.skillResult().trim();
        }
        SkillAnalysisReportVO skillReport = JsonParseUtil.toBeanQuiet(jsonForSkillReport, SkillAnalysisReportVO.class);
        if (skillReport == null) {
            log.error("技能最终结果无法解析为报告 JSON，sessionId={}", context.sessionId());
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "技能报告解析失败，请稍后重试。");
        }
        skillReport.setName(context.session().getName());
        skillReport.setTargetJob(context.targetJob());
        skillReport.setSkill(context.skill());
        skillReport.setTestTime(LocalDateTime.now());
        persistence.updateSkillAdjustedAsync(context.sessionId(), context.userId(), AdjustedStoragePayload.fromReport(skillReport));
        knowledgeGraphTalentSyncService.syncAfterSkillTestComplete(context.userId(), context.session());

        if (workflowLifecycleService.isEnabled()) {
            workflowLifecycleService.onSkillComplete(context.sessionId(), jsonForSkillReport);
            cozeBotInvoker.resetConversationIfNeeded(context.sessionId(), AgentType.CAREER_TEST, FlowPhase.CAREER_ACTIVE);
        }

        String careerRaw = resolveCareerQuestionsRaw(context.sessionId(), context.session());
        if (StrUtil.isNotBlank(careerRaw)) {
            context.session().setCareerQuestion(careerRaw);
            if (workflowLifecycleService.isEnabled()) {
                SessionWorkflowState wfState = workflowEngine.getState(context.sessionId());
                if (StrUtil.isBlank(wfState.getCareerQuestionsJson())) {
                    workflowEngine.storeCareerQuestions(context.sessionId(), careerRaw);
                }
            }
            contextManager.setMode(AgentType.CAREER_TEST, context.sessionId());
            contextManager.setCareerTriggered(context.sessionId(), true);
        } else {
            careerTestAgent.doChat("你好", context.sessionId());
            careerRaw = context.session().getCareerQuestion();
            if (StrUtil.isNotBlank(careerRaw) && workflowLifecycleService.isEnabled()) {
                workflowEngine.storeCareerQuestions(context.sessionId(), careerRaw);
            }
            if (StrUtil.isNotBlank(careerRaw)) {
                contextManager.setMode(AgentType.CAREER_TEST, context.sessionId());
                contextManager.setCareerTriggered(context.sessionId(), true);
            }
        }
        if (context.session().getCareerQuestion() != null) {
            persistence.saveCareer(context.userId(), context.sessionId(), context.session().getCareerQuestion(), "QUESTION");
        }

        CareerTestQuestionDto careerTestQuestionDto = ExperienceQuestionParser.parseCareerQuestions(context.session().getCareerQuestion());
        ChatPayload payload = ChatPayload.builder()
                .sessionId(context.sessionId())
                .content(CAREER_HANDOFF_MESSAGE)
                .skillTestReady(false)
                .careerTestReady(contextManager.getMode(context.sessionId()) == AgentType.CAREER_TEST && careerTestQuestionDto != null)
                .careerTestQuestions(careerTestQuestionDto)
                .skillReport(skillReport)
                .build();
        return workflowLifecycleService.enrich(payload, context.sessionId());
    }

    private String resolveCareerQuestionsRaw(String sessionId, AssessmentStore.SessionAssessment session) {
        if (workflowLifecycleService.isEnabled()) {
            String fromWorkflow = workflowEngine.getState(sessionId).getCareerQuestionsJson();
            if (StrUtil.isNotBlank(fromWorkflow)) {
                return fromWorkflow;
            }
        }
        return session.getCareerQuestion();
    }
}
