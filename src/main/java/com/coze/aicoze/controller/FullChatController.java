package com.coze.aicoze.controller;

import com.coze.aicoze.annotation.Idempotent;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.common.Result;
import com.coze.aicoze.dto.AnswerRequest;
import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.ChatMessageRequest;
import com.coze.aicoze.dto.full.FullChatPayload;
import com.coze.aicoze.service.FullAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 完全版对话与测评（多轮技能测评、独立 {@link FullChatPayload}）。
 * 体验版请使用 {@link ChatController}，避免混用路由以便维护。
 * <p>当 {@code app.full-version-temporarily-closed=true} 时接口返回固定提示；设为 {@code false} 即开放完全版。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/full/chat")
public class FullChatController {

    private static final String FULL_VERSION_UNAVAILABLE_MSG = "该功能还在开放中，暂不开放";

    private final FullAssessmentService fullAssessmentService;

    @Value("${app.full-version-temporarily-closed:true}")
    private boolean fullVersionTemporarilyClosed;

    @PostMapping("/init")
    public Result<FullChatPayload> init(@RequestBody ChatInitRequest request) {
        if (fullVersionTemporarilyClosed) {
            return fullUnavailable();
        }
        return Result.success(fullAssessmentService.initAndFirstChat(request));
    }

    @Idempotent(required = false)
    @PostMapping("/send")
    public Result<FullChatPayload> sendMessage(@RequestBody ChatMessageRequest request) {
        if (fullVersionTemporarilyClosed) {
            return fullUnavailable();
        }
        return Result.success(fullAssessmentService.chat(request.getSessionId(), request.getMessage()));
    }

    @Idempotent
    @PostMapping("/skill/answer")
    public Result<FullChatPayload> submitSkillAnswer(@RequestBody AnswerRequest request) {
        if (fullVersionTemporarilyClosed) {
            return fullUnavailable();
        }
        return Result.success(fullAssessmentService.submitSkillAnswer(request.getSessionId(), request.getAnswer()));
    }

    @Idempotent
    @PostMapping("/career/answer")
    public Result<FullChatPayload> submitCareerAnswer(@RequestBody AnswerRequest request) {
        if (fullVersionTemporarilyClosed) {
            return fullUnavailable();
        }
        return Result.success(fullAssessmentService.submitCareerAnswer(request.getSessionId(), request.getAnswer()));
    }

    private static Result<FullChatPayload> fullUnavailable() {
        return Result.error(BizErrorCodes.FEATURE_TEMPORARILY_UNAVAILABLE, FULL_VERSION_UNAVAILABLE_MSG);
    }
}
