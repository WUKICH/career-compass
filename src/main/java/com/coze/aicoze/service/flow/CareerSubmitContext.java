package com.coze.aicoze.service.flow;

import com.coze.aicoze.manager.AssessmentStore;

public record CareerSubmitContext(
        String sessionId,
        AssessmentStore.SessionAssessment session,
        Integer userId,
        String careerResult
) {
}
