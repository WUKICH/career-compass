package com.coze.aicoze.service;

import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.dto.FeedbackRequest;
import com.coze.aicoze.vo.CareerAssessmentVO;

public interface AssessmentService {

    ChatPayload initAndFirstChat(ChatInitRequest request);

    ChatPayload chat(String sessionId, String message);

    ChatPayload startSkillAssessment(String sessionId);

    /** 查询 workflow 状态（题目预生成进度、是否可开始测评等） */
    ChatPayload getWorkflowStatus(String sessionId);

    ChatPayload submitSkillAnswer(String sessionId, String answer);

    ChatPayload submitCareerAnswer(String sessionId, String answer);

    void submitFeedback(FeedbackRequest feedbackRequest);

    String semanticAnalysis(String sessionId, String agentQuestion, String userAnswer);

    /** 按 scope 返回阶段性综合报告（resume / skill / career / full） */
    CareerAssessmentVO getPartialReport(String sessionId, String scope);

    com.coze.aicoze.vo.HollandAnalysisVO getHollandReport(String sessionId);

}



