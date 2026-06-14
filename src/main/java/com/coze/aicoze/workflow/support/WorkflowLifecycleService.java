package com.coze.aicoze.workflow.support;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.workflow.composer.ReportComposer;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.FlowCommand;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.FlowVariant;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import com.coze.aicoze.service.chat.SemanticAnalysisService;
import com.coze.aicoze.workflow.guard.MainChatInputGuard;
import com.coze.aicoze.workflow.router.CozeIntentRouter;
import com.coze.aicoze.workflow.router.IntentRouteResult;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * WorkflowEngine 与现有 Assessment 服务的衔接层。
 */
@Component
@RequiredArgsConstructor
public class WorkflowLifecycleService {

    private final WorkflowProperties workflowProperties;
    private final WorkflowEngine workflowEngine;
    private final CozeIntentRouter intentRouter;
    private final ReportComposer reportComposer;
    private final PromptFactory promptFactory;
    private final MainChatInputGuard mainChatInputGuard;
    private final SemanticAnalysisService semanticAnalysisService;
    private final WorkflowContextSync workflowContextSync;

    public boolean isEnabled() {
        return true;
    }

    public void onInitConfirmed(String sessionId, Integer userId, ChatInitRequest request) {
        String anchor = buildAnchorJson(request);
        workflowEngine.onProfileConfirmed(sessionId, userId, anchor);
        workflowContextSync.syncForPhase(sessionId, FlowPhase.MAIN_CHAT);
    }

    public void onFullInitConfirmed(String sessionId, Integer userId, ChatInitRequest request) {
        String anchor = buildAnchorJson(request);
        workflowEngine.onProfileConfirmed(sessionId, userId, anchor, FlowVariant.FULL);
        workflowContextSync.syncForPhase(sessionId, FlowPhase.MAIN_CHAT);
    }

    public void assertMainChat(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.assertCommandAllowed(sessionId, FlowCommand.MAIN_CHAT_MESSAGE);
    }

    public void assertSkillSubmit(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.assertCommandAllowed(sessionId, FlowCommand.SUBMIT_SKILL_ANSWER);
    }

    public void assertCareerSubmit(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.assertCommandAllowed(sessionId, FlowCommand.SUBMIT_CAREER_ANSWER);
    }

    public void assertStartSkill(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.assertCommandAllowed(sessionId, FlowCommand.START_SKILL);
    }

    /**
     * 主聊前置：IntentRouter。若 OFF_TOPIC 高置信则返回拦截文案。
     */
    public Optional<ChatPayload> preMainChat(String sessionId, String userMessage, String agentQuestion) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        if (state.getPhase() != FlowPhase.MAIN_CHAT) {
            return Optional.empty();
        }
        Optional<String> localBlock = mainChatInputGuard.checkLocal(userMessage);
        if (localBlock.isPresent()) {
            return Optional.of(enrich(
                    ChatPayload.builder().content(localBlock.get()).sessionId(sessionId).build(), sessionId));
        }
        if (workflowProperties.getMainChat().isSemanticGateEnabled()
                && StrUtil.isNotBlank(agentQuestion)) {
            String semanticBlock = semanticAnalysisService.analyze(sessionId, agentQuestion, userMessage);
            if (StrUtil.isNotBlank(semanticBlock)) {
                return Optional.of(enrich(
                        ChatPayload.builder().content(semanticBlock).sessionId(sessionId).build(), sessionId));
            }
        }
        IntentRouteResult route = intentRouter.classify(state, userMessage, agentQuestion);
        workflowEngine.applyIntentSuggestion(sessionId, route.isReadyForSkill(), route.getConfidence());
        if (route.isOffTopic() && route.getConfidence() >= workflowProperties.getIntent().getReadySkillConfidence()) {
            String reply = StrUtil.isBlank(route.getReply())
                    ? "这条回复和当前话题不太相关，可以围绕职业目标或技能继续聊聊～"
                    : route.getReply();
            return Optional.of(enrich(ChatPayload.builder().content(reply).sessionId(sessionId).build(), sessionId));
        }
        return Optional.empty();
    }

    public void afterMainChatTurn(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.incrementMainChatTurn(sessionId);
    }

    public SessionWorkflowState startSkillPhase(String sessionId) {
        return workflowEngine.startSkill(sessionId);
    }

    public void onSkillQuestionsPrepared(String sessionId, String questionsJson) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.storeSkillQuestions(sessionId, questionsJson);
    }

    public void onCareerQuestionsPrepared(String sessionId, String questionsJson) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.storeCareerQuestions(sessionId, questionsJson);
    }

    public void onSkillComplete(String sessionId, String skillSummaryJson) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.completeSkill(sessionId, skillSummaryJson);
        workflowEngine.startCareer(sessionId);
    }

    public void onCareerComplete(String sessionId, String careerSummaryJson) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.completeCareer(sessionId, careerSummaryJson);
    }

    public void onFeedback(String sessionId) {
        if (!isEnabled()) {
            return;
        }
        workflowEngine.close(sessionId);
    }

    public ChatPayload enrich(ChatPayload payload, String sessionId) {
        if (!isEnabled() || payload == null) {
            return payload;
        }
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        ChatPayload.ChatPayloadBuilder b = ChatPayload.builder()
                .sessionId(payload.getSessionId() != null ? payload.getSessionId() : sessionId)
                .content(payload.getContent())
                .presetOptions(payload.getPresetOptions())
                .skillTestReady(payload.isSkillTestReady())
                .careerTestReady(payload.isCareerTestReady())
                .token(payload.getToken())
                .skillReport(payload.getSkillReport())
                .hollandReport(payload.getHollandReport())
                .careerAssessmentReport(payload.getCareerAssessmentReport())
                .skillTestQuestions(payload.getSkillTestQuestions())
                .careerTestQuestions(payload.getCareerTestQuestions())
                .preliminaryRecommendation(payload.getPreliminaryRecommendation())
                .consensusResult(payload.getConsensusResult());
        reportComposer.enrichPayload(b, state, payload);
        return b.build();
    }

    /** 供前端轮询：题目预生成完成后 suggestStartSkill 会变为 true。 */
    public ChatPayload getWorkflowStatus(String sessionId) {
        if (!isEnabled()) {
            return ChatPayload.builder().sessionId(sessionId).build();
        }
        return enrich(ChatPayload.builder().sessionId(sessionId).build(), sessionId);
    }

    public void preGenerateQuestionsAsync(String sessionId, AssessmentStore.SessionAssessment session) {
        if (!isEnabled() || session == null) {
            return;
        }
        // 占位：CONFIRM 后由 AssessmentService 异步调 Skill/Career Agent 预生成（见 QuestionPreparationService）
    }

    private String buildAnchorJson(ChatInitRequest request) {
        return JSONUtil.toJsonStr(new AnchorProfile(
                request.getName(),
                request.getWorkYears(),
                request.getTargetJob(),
                request.getSkill(),
                request.getExpectedCity(),
                request.getExpectedSalary()));
    }

    public String prependAnchor(String sessionId, String userMessage, AssessmentStore.SessionAssessment session) {
        if (!isEnabled() || session == null) {
            return userMessage;
        }
        SessionWorkflowState state = workflowEngine.getState(sessionId);
        String anchor = state.getAnchorProfileJson();
        if (StrUtil.isBlank(anchor)) {
            anchor = promptFactory.buildFirstMessage(
                    session.getExperienceVersion(),
                    session.getWorkYears(),
                    session.getSkill(),
                    session.getTargetJob(),
                    session.getExpectedCity(),
                    session.getExpectedSalary());
        }
        return anchor + "\n\n用户消息：" + userMessage;
    }

    private record AnchorProfile(String name, String workYears, String targetJob, String skill,
                                 String expectedCity, String expectedSalary) {
    }
}
