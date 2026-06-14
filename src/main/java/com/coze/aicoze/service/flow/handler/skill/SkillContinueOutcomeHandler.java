package com.coze.aicoze.service.flow.handler.skill;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.dto.experience.SkillTestQuestionDto;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.flow.SkillSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.service.flow.handler.SkillSubmitOutcomeHandler;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import com.coze.aicoze.util.ExperienceQuestionParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillContinueOutcomeHandler implements SkillSubmitOutcomeHandler {
    private final AssessmentPersistence persistence;
    private final ContextManager contextManager;
    private final WorkflowLifecycleService workflowLifecycleService;

    @Override
    public TransitionOutcome outcome() {
        return TransitionOutcome.CONTINUE;
    }

    @Override
    public ChatPayload handle(SkillSubmitContext context) {
        context.session().setSkillQuestion(context.skillResult());
        persistence.saveSkill(context.userId(), context.sessionId(), context.skillResult(), "SKILL_QUESTION");
        SkillTestQuestionDto skillDto = ExperienceQuestionParser.parseSkillQuestions(context.skillResult());
        return workflowLifecycleService.enrich(ChatPayload.builder()
                .sessionId(context.sessionId())
                .content("请继续完成技能测试。")
                .skillTestReady(contextManager.getMode(context.sessionId()) == AgentType.SKILL_TEST && skillDto != null)
                .careerTestReady(false)
                .skillTestQuestions(skillDto)
                .build(), context.sessionId());
    }
}
