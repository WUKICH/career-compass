package com.coze.aicoze.service.flow.handler;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.service.flow.CareerSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;

public interface CareerSubmitOutcomeHandler {
    TransitionOutcome outcome();

    ChatPayload handle(CareerSubmitContext context);
}
