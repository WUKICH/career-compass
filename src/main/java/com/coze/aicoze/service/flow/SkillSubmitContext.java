package com.coze.aicoze.service.flow;

import com.coze.aicoze.manager.AssessmentStore;

public record SkillSubmitContext(
        String sessionId,
        AssessmentStore.SessionAssessment session,
        Integer userId,
        String skillResult,
        String skill,
        String targetJob
) {
}
