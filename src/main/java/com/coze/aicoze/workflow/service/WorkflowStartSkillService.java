package com.coze.aicoze.workflow.service;



import cn.hutool.core.util.StrUtil;

import com.coze.aicoze.agent.bots.SkillTestAgent;

import com.coze.aicoze.common.BizErrorCodes;

import com.coze.aicoze.dto.ChatPayload;

import com.coze.aicoze.dto.experience.SkillTestQuestionDto;

import com.coze.aicoze.enums.AgentType;

import com.coze.aicoze.exception.BusinessException;

import com.coze.aicoze.manager.AssessmentStore;

import com.coze.aicoze.manager.ContextManager;

import com.coze.aicoze.util.ExperienceQuestionParser;

import com.coze.aicoze.util.PromptFactory;

import com.coze.aicoze.workflow.engine.WorkflowEngine;

import com.coze.aicoze.workflow.invoker.CozeBotInvoker;

import com.coze.aicoze.workflow.model.FlowPhase;

import com.coze.aicoze.workflow.model.SessionWorkflowState;

import com.coze.aicoze.workflow.support.WorkflowLifecycleService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;



@Service

@RequiredArgsConstructor

public class WorkflowStartSkillService {



    private final WorkflowLifecycleService workflowLifecycle;

    private final WorkflowEngine workflowEngine;

    private final AssessmentStore assessmentStore;

    private final SkillTestAgent skillTestAgent;

    private final ContextManager contextManager;

    private final CozeBotInvoker cozeBotInvoker;

    private final PromptFactory promptFactory;



    public ChatPayload startSkill(String sessionId) {

        workflowLifecycle.assertStartSkill(sessionId);

        workflowEngine.startSkill(sessionId);

        cozeBotInvoker.resetConversationIfNeeded(sessionId, AgentType.SKILL_TEST, FlowPhase.SKILL_ACTIVE);

        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);

        SessionWorkflowState state = workflowEngine.getState(sessionId);



        String skillStored = state.getSkillQuestionsJson();

        if (StrUtil.isBlank(skillStored)) {

            skillStored = session.getSkillQuestion();

        }

        SkillTestQuestionDto dto = ExperienceQuestionParser.parseSkillQuestions(skillStored);

        if (dto == null) {

            String prompt = promptFactory.buildSkillTestPrompt(session.getSkill(), session.getTargetJob());

            skillTestAgent.doChat(prompt, sessionId);

            skillStored = session.getSkillQuestion();

            workflowEngine.storeSkillQuestions(sessionId, skillStored);

            dto = ExperienceQuestionParser.parseSkillQuestions(skillStored);

        }

        if (dto == null) {

            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE,

                    "技能测评题目生成失败，请稍后重试或联系管理员检查技能 Bot 是否按 JSON 格式出题。");

        }

        session.setSkillQuestion(skillStored);

        contextManager.setMode(AgentType.SKILL_TEST, sessionId);

        contextManager.setSkillTriggered(sessionId, true);



        ChatPayload payload = ChatPayload.builder()

                .sessionId(sessionId)

                .skillTestReady(true)

                .skillTestQuestions(dto)

                .careerTestReady(false)

                .build();

        return workflowLifecycle.enrich(payload, sessionId);

    }

}


