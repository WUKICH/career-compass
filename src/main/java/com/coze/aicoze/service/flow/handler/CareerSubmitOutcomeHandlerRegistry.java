package com.coze.aicoze.service.flow.handler;

import com.coze.aicoze.service.flow.TransitionOutcome;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CareerSubmitOutcomeHandlerRegistry {
    private final Map<TransitionOutcome, CareerSubmitOutcomeHandler> registry;

    public CareerSubmitOutcomeHandlerRegistry(List<CareerSubmitOutcomeHandler> handlers) {
        EnumMap<TransitionOutcome, CareerSubmitOutcomeHandler> map = new EnumMap<>(TransitionOutcome.class);
        for (CareerSubmitOutcomeHandler handler : handlers) {
            if (map.put(handler.outcome(), handler) != null) {
                throw new IllegalStateException("Duplicate CareerSubmitOutcomeHandler for outcome: " + handler.outcome());
            }
        }
        this.registry = map;
    }

    public CareerSubmitOutcomeHandler get(TransitionOutcome outcome) {
        CareerSubmitOutcomeHandler handler = registry.get(outcome);
        if (handler == null) {
            throw new IllegalStateException("No CareerSubmitOutcomeHandler for outcome: " + outcome);
        }
        return handler;
    }
}
