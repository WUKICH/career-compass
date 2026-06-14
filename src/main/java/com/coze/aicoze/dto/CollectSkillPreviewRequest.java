package com.coze.aicoze.dto;

import lombok.Data;

/**
 * 核心技能提交后预览结构化画像（先 NLP + 收集 Agent，不落库）。
 */
@Data
public class CollectSkillPreviewRequest {
    private String sessionId;
    /** 用户在聊天中输入的核心技能原文 */
    private String userInput;
}
