package com.coze.aicoze.service.flow.handler.career;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.flow.CareerSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.service.flow.handler.CareerSubmitOutcomeHandler;
import com.coze.aicoze.util.ExperienceQuestionParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareerContinueOutcomeHandler implements CareerSubmitOutcomeHandler {
    private final AssessmentPersistence persistence;
    private final ContextManager contextManager;

    @Override
    public TransitionOutcome outcome() {
        return TransitionOutcome.CONTINUE;
    }

    @Override
    public ChatPayload handle(CareerSubmitContext context) {
        context.session().setCareerQuestion(context.careerResult());
        persistence.saveCareer(context.userId(), context.sessionId(), context.careerResult(), "QUESTION");
        CareerTestQuestionDto careerDto = ExperienceQuestionParser.parseCareerQuestions(context.careerResult());
        return ChatPayload.builder()
                .sessionId(context.sessionId())
                .content("请继续完成霍兰德测试。")
                .skillTestReady(false)
                .careerTestReady(contextManager.getMode(context.sessionId()) == AgentType.CAREER_TEST && careerDto != null)
                .careerTestQuestions(careerDto)
                .build();
    }
}
