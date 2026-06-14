package com.coze.aicoze.workflow.router;

import com.coze.aicoze.workflow.model.SessionWorkflowState;

public interface IntentRouter {

    IntentRouteResult classify(SessionWorkflowState state, String userMessage, String recentQuestionSummary);
}
