package com.coze.aicoze.workflow.engine;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.workflow.audit.FlowEventLogger;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.aicoze.workflow.graph.LangGraphWorkflowAdapter;
import com.coze.aicoze.workflow.model.FlowCommand;
import com.coze.aicoze.workflow.model.FlowVariant;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import com.coze.aicoze.workflow.store.WorkflowStateStore;
import com.coze.aicoze.workflow.support.WorkflowContextSync;
import com.coze.aicoze.service.component.AssessmentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowStateStore stateStore;
    private final FlowEventLogger flowEventLogger;
    private final WorkflowProperties workflowProperties;
    private final WorkflowContextSync workflowContextSync;
    private final LangGraphWorkflowAdapter langGraphWorkflowAdapter;
    private final AssessmentSessionService assessmentSessionService;

    public SessionWorkflowState getState(String sessionId) {
        return stateStore.getOrCreate(sessionId);
    }

    public void assertCommandAllowed(String sessionId, FlowCommand command) {
        SessionWorkflowState state = getState(sessionId);
        if (!PhaseGuard.isAllowed(command, state.getPhase())) {
            flowEventLogger.logRejected(sessionId, state.getPhase(), command.name(),
                    "phase mismatch");
            throw new BusinessException(BizErrorCodes.INVALID_WORKFLOW_PHASE,
                    "当前阶段不允许该操作，phase=" + state.getPhase());
        }
    }

    public SessionWorkflowState transition(String sessionId, FlowPhase to, String trigger) {
        SessionWorkflowState state = getState(sessionId);
        FlowPhase from = state.getPhase();
        if (from == to) {
            return state;
        }
        langGraphWorkflowAdapter.assertTransitionAllowed(state, from, to);
        state.setPhase(to);
        if (to == FlowPhase.MAIN_CHAT) {
            state.setSuggestStartSkill(false);
        }
        stateStore.save(state);
        assessmentSessionService.updatePhase(sessionId, to);
        flowEventLogger.logTransition(sessionId, state.getUserId(), from, to, trigger, null);
        workflowContextSync.syncForPhase(sessionId, to);
        return state;
    }

    public void onProfileConfirmed(String sessionId, Integer userId, String anchorProfileJson) {
        onProfileConfirmed(sessionId, userId, anchorProfileJson, FlowVariant.TRIAL);
    }

    public void onProfileConfirmed(String sessionId, Integer userId, String anchorProfileJson, FlowVariant variant) {
        SessionWorkflowState state = getState(sessionId);
        state.setUserId(userId);
        state.setAnchorProfileJson(anchorProfileJson);
        state.setVariant(variant == null ? FlowVariant.TRIAL : variant);
        stateStore.save(state);
        transition(sessionId, FlowPhase.MAIN_CHAT, "CONFIRM_PROFILE");
    }

    /** 完全版：技能多轮 continue 时保持 SKILL_ACTIVE 并递增轮次。 */
    public void continueSkillRound(String sessionId) {
        SessionWorkflowState state = getState(sessionId);
        if (state.getPhase() != FlowPhase.SKILL_ACTIVE) {
            return;
        }
        state.setSkillRound(state.getSkillRound() + 1);
        stateStore.save(state);
        flowEventLogger.logTransition(sessionId, state.getUserId(), FlowPhase.SKILL_ACTIVE, FlowPhase.SKILL_ACTIVE,
                "SKILL_CONTINUE", "round=" + state.getSkillRound());
    }

    public void incrementMainChatTurn(String sessionId) {
        SessionWorkflowState state = getState(sessionId);
        state.setMainChatTurnCount(state.getMainChatTurnCount() + 1);
        stateStore.save(state);
    }

    public void applyIntentSuggestion(String sessionId, boolean readyForSkill, double confidence) {
        SessionWorkflowState state = getState(sessionId);
        if (state.getPhase() != FlowPhase.MAIN_CHAT) {
            return;
        }
        boolean suggest = readyForSkill
                && confidence >= workflowProperties.getIntent().getReadySkillConfidence()
                && state.getMainChatTurnCount() >= workflowProperties.getIntent().getMinMainChatTurns();
        state.setSuggestStartSkill(suggest);
        stateStore.save(state);
    }

    public SessionWorkflowState startSkill(String sessionId) {
        assertCommandAllowed(sessionId, FlowCommand.START_SKILL);
        SessionWorkflowState state = transition(sessionId, FlowPhase.SKILL_ACTIVE, "START_SKILL");
        state.setSkillRound(state.getSkillRound() + 1);
        state.setSuggestStartSkill(false);
        stateStore.save(state);
        return state;
    }

    public SessionWorkflowState completeSkill(String sessionId, String skillSummaryJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setSkillSummaryJson(skillSummaryJson);
        stateStore.save(state);
        return transition(sessionId, FlowPhase.SKILL_DONE, "SKILL_COMPLETE");
    }

    public SessionWorkflowState startCareer(String sessionId) {
        SessionWorkflowState state = transition(sessionId, FlowPhase.CAREER_ACTIVE, "START_CAREER");
        stateStore.save(state);
        return state;
    }

    public SessionWorkflowState completeCareer(String sessionId, String careerSummaryJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setCareerSummaryJson(careerSummaryJson);
        stateStore.save(state);
        return transition(sessionId, FlowPhase.CAREER_DONE, "CAREER_COMPLETE");
    }

    public SessionWorkflowState enterPreliminaryRec(String sessionId) {
        return transition(sessionId, FlowPhase.PRELIMINARY_REC, "ENTER_PRELIMINARY_REC");
    }

    public SessionWorkflowState startConsensus(String sessionId) {
        return transition(sessionId, FlowPhase.CONSENSUS_ACTIVE, "START_CONSENSUS");
    }

    public void storePreliminaryRec(String sessionId, String preliminaryRecJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setPreliminaryRecJson(preliminaryRecJson);
        stateStore.save(state);
    }

    public void storeConsensusResult(String sessionId, String consensusResultJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setConsensusResultJson(consensusResultJson);
        stateStore.save(state);
    }

    public SessionWorkflowState markReportReady(String sessionId) {
        return transition(sessionId, FlowPhase.REPORT_READY, "GENERATE_REPORT");
    }

    public void close(String sessionId) {
        transition(sessionId, FlowPhase.CLOSED, "SUBMIT_FEEDBACK");
        assessmentSessionService.markCompleted(sessionId);
        stateStore.clear(sessionId);
    }

    public void storeSkillQuestions(String sessionId, String questionsJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setSkillQuestionsJson(questionsJson);
        state.setSkillQuestionsPrepared(true);
        if (state.getPhase() == FlowPhase.MAIN_CHAT) {
            state.setSuggestStartSkill(true);
        }
        stateStore.save(state);
    }

    public void storeCareerQuestions(String sessionId, String questionsJson) {
        SessionWorkflowState state = getState(sessionId);
        state.setCareerQuestionsJson(questionsJson);
        state.setCareerQuestionsPrepared(true);
        stateStore.save(state);
    }

    /** 体验版确认后：主聊阶段直接展示「开始能力测评」入口，无需先聊多轮。 */
    public void suggestStartSkillAfterConfirm(String sessionId) {
        SessionWorkflowState state = getState(sessionId);
        if (state.getPhase() != FlowPhase.MAIN_CHAT || !state.isSkillQuestionsPrepared()) {
            return;
        }
        state.setSuggestStartSkill(true);
        stateStore.save(state);
    }
}
