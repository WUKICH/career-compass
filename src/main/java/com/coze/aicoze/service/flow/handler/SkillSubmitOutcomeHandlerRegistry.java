package com.coze.aicoze.service.flow.handler;

import com.coze.aicoze.service.flow.TransitionOutcome;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SkillSubmitOutcomeHandlerRegistry {
    private final Map<TransitionOutcome, SkillSubmitOutcomeHandler> registry;

    public SkillSubmitOutcomeHandlerRegistry(List<SkillSubmitOutcomeHandler> handlers) {
        EnumMap<TransitionOutcome, SkillSubmitOutcomeHandler> map = new EnumMap<>(TransitionOutcome.class);
        for (SkillSubmitOutcomeHandler handler : handlers) {
            if (map.put(handler.outcome(), handler) != null) {
                throw new IllegalStateException("Duplicate SkillSubmitOutcomeHandler for outcome: " + handler.outcome());
            }
        }
        this.registry = map;
    }

    public SkillSubmitOutcomeHandler get(TransitionOutcome outcome) {
        SkillSubmitOutcomeHandler handler = registry.get(outcome);
        if (handler == null) {
            throw new IllegalStateException("No SkillSubmitOutcomeHandler for outcome: " + outcome);
        }
        return handler;
    }
}
