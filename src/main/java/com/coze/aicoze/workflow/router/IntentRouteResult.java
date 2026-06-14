package com.coze.aicoze.workflow.router;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntentRouteResult {
    private String intent;
    private double confidence;
    private String reply;
    private boolean readyForSkill;
    private boolean offTopic;
}
