package com.coze.aicoze.service.flow.handler;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.service.flow.SkillSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;

public interface SkillSubmitOutcomeHandler {
    TransitionOutcome outcome();

    ChatPayload handle(SkillSubmitContext context);
}
