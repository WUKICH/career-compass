package com.coze.aicoze.full.flow;

import com.coze.aicoze.agent.bots.CareerTestAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class FullRoundHollandAgent {

    private final CareerTestAgent careerTestAgent;

    public CompletableFuture<String> submitAnswer(String sessionId, Integer userId, String answer) {
        return careerTestAgent.careerAssessmentAsync(sessionId, userId, answer);
    }
}
