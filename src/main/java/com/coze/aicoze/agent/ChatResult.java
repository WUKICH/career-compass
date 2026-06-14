package com.coze.aicoze.agent;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResult {
    private String content;
    private boolean skillTriggered;
    private boolean careerTriggered;
    /** AI 返回的快捷选项，会通过 ChatPayload 传给前端展示 */
    private List<String> presetOptions;
}



