package com.coze.aicoze.full.flow;

import com.coze.aicoze.agent.bots.SkillTestAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class FullRoundSkillAgent {

    private final SkillTestAgent skillTestAgent;

    public CompletableFuture<String> submitAnswer(String sessionId, Integer userId, String answer) {
        return skillTestAgent.skillAssessmentAsync(sessionId, userId, answer);
    }
}
