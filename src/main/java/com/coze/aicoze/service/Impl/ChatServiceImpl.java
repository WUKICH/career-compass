package com.coze.aicoze.service.Impl;

import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.bots.ChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.manager.RouterManager;
import com.coze.aicoze.service.ChatService;
import com.coze.aicoze.util.ExperienceQuestionParser;
import com.coze.openapi.client.chat.model.ChatUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RouterManager routerManager;
    private final AssessmentStore assessmentStore;
    private final ContextManager contextManager;

    @Override
    public String startNewChat() {
        return "";
    }

    @Override
    public ChatPayload sendMessage(String sessionId, String userMessage) {
        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
        BaseChatAgent routeAgent = routerManager.route(sessionId);
        ChatResult result = routeAgent.doChat(userMessage, sessionId);
        var skillDto = session.getSkillQuestion() != null
                ? ExperienceQuestionParser.parseSkillQuestions(session.getSkillQuestion())
                : null;
        var careerDto = session.getCareerQuestion() != null
                ? ExperienceQuestionParser.parseCareerQuestions(session.getCareerQuestion())
                : null;
        AgentType mode = contextManager.getMode(sessionId);
        return ChatPayload.builder()
                .content(result.getContent())
                .skillTestReady(mode == AgentType.SKILL_TEST && skillDto != null)
                .careerTestReady(mode == AgentType.CAREER_TEST && careerDto != null)
                .skillTestQuestions(skillDto)
                .careerTestQuestions(careerDto)
                .build();
    }

    @Override
    public void streamChat(String sessionId, String userMessage, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        ChatAgent routeAgent = (ChatAgent) routerManager.route(sessionId);
        routeAgent.doChatStream(userMessage, sessionId, onContent, onComplete);
    }
}
