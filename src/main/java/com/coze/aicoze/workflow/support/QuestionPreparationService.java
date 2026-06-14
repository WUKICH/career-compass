package com.coze.aicoze.workflow.support;



import cn.hutool.core.util.StrUtil;

import com.coze.aicoze.agent.bots.CareerTestAgent;

import com.coze.aicoze.agent.bots.SkillTestAgent;

import com.coze.aicoze.manager.AssessmentStore;

import com.coze.aicoze.util.ExperienceQuestionParser;

import com.coze.aicoze.util.PromptFactory;

import com.coze.aicoze.workflow.engine.WorkflowEngine;

import com.coze.aicoze.workflow.model.FlowPhase;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;



import java.util.concurrent.CompletableFuture;

import java.util.concurrent.Executor;



@Component

@Slf4j

@RequiredArgsConstructor

public class QuestionPreparationService {



    private final WorkflowLifecycleService workflowLifecycle;

    private final WorkflowEngine workflowEngine;

    private final WorkflowContextSync workflowContextSync;

    private final SkillTestAgent skillTestAgent;

    private final CareerTestAgent careerTestAgent;

    private final PromptFactory promptFactory;

    private final AssessmentStore assessmentStore;

    private final Executor assessmentExecutor;



    public void prepareAfterConfirm(String sessionId) {

        if (!workflowLifecycle.isEnabled()) {

            return;

        }

        CompletableFuture.runAsync(() -> doPrepare(sessionId), assessmentExecutor);

    }



    private void doPrepare(String sessionId) {

        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);

        if (session == null) {

            return;

        }

        FlowPhase phaseBefore = workflowEngine.getState(sessionId).getPhase();

        try {

            String skillPrompt = promptFactory.buildSkillTestPrompt(session.getSkill(), session.getTargetJob());

            skillTestAgent.doChat(skillPrompt, sessionId);

            String skillRaw = session.getSkillQuestion();

            if (StrUtil.isNotBlank(skillRaw) && ExperienceQuestionParser.parseSkillQuestions(skillRaw) != null) {

                workflowLifecycle.onSkillQuestionsPrepared(sessionId, skillRaw);

            } else {

                log.warn("Pre-generate skill questions invalid JSON, sessionId={}", sessionId);

            }

        } catch (Exception e) {

            log.warn("Pre-generate skill questions failed, sessionId={}", sessionId, e);

        }

        try {

            String careerPrompt = promptFactory.buildCareerTestPrompt(session.getSkill(), session.getTargetJob());

            careerTestAgent.doChat(careerPrompt, sessionId);

            String careerRaw = session.getCareerQuestion();

            if (StrUtil.isNotBlank(careerRaw) && ExperienceQuestionParser.parseCareerQuestions(careerRaw) != null) {

                workflowLifecycle.onCareerQuestionsPrepared(sessionId, careerRaw);
                log.info("Pre-generate career questions stored, sessionId={}", sessionId);

            } else {

                log.warn("Pre-generate career questions invalid JSON, sessionId={}", sessionId);

            }

        } catch (Exception e) {

            log.warn("Pre-generate career questions failed, sessionId={}", sessionId, e);

        }

        workflowContextSync.syncForPhase(sessionId, phaseBefore);

    }

}


