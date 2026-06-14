package com.coze.aicoze.service;

import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.full.FullChatPayload;

/**
 * 完全版测评与对话：多轮技能测评、独立响应结构 {@link com.coze.aicoze.dto.full.FullChatPayload}，
 * 与 {@link AssessmentService}（体验版）分流维护。
 */
public interface FullAssessmentService {

    FullChatPayload initAndFirstChat(ChatInitRequest request);

    FullChatPayload chat(String sessionId, String message);

    FullChatPayload submitSkillAnswer(String sessionId, String answer);

    FullChatPayload submitCareerAnswer(String sessionId, String answer);
}
