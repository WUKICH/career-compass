package com.coze.aicoze.controller;

import com.coze.aicoze.annotation.Idempotent;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.common.Result;
import com.coze.aicoze.common.SseErrorJson;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.dto.*;
import com.coze.aicoze.service.CollectService;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.CollectStepPayload;
import com.coze.aicoze.service.AssessmentService;
import com.coze.aicoze.service.ChatService;
import com.coze.aicoze.util.ResumeDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Locale;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final AssessmentService assessmentService;
    private final CollectService collectService;
//    @PostMapping("/start")
//    public String startNewChat() {
//        return chatService.startNewChat();
//    }
    @GetMapping("/test")
    public String test() {
        return "ChatEndResult service is running!";
    }

    @PostMapping("/init")
    public Result<ChatPayload> init(@RequestBody ChatInitRequest request) {
        return Result.success(assessmentService.initAndFirstChat(request));
    }

    /** 对话式收集个人信息一步：questionKey + userInput；不发起确认/落库，完成后请调 {@code /chat/collect/confirm} */
    @PostMapping("/collect")
    public Result<CollectStepPayload> collectStep(@RequestBody CollectStepRequest request) {
        return Result.success(collectService.collectStep(request));
    }

    /** 信息确认页提交：sessionId + answers，校验通过后落库并返回 sessionId + token */
    @Idempotent
    @PostMapping("/collect/confirm")
    public Result<CollectStepPayload> confirmCollect(@RequestBody CollectConfirmRequest request) {
        return Result.success(collectService.confirmCollect(request));
    }

    /** 核心技能输入后：NLP + 收集 Agent 返回结构化画像，供确认页展示（不落库） */
    @PostMapping("/collect/preview-skill")
    public Result<CollectStepPayload> previewSkill(@RequestBody CollectSkillPreviewRequest request) {
        return Result.success(collectService.previewSkillProfile(request));
    }

    /** 上传简历文本后：调用简历解析 Agent 返回结构化画像，供确认页展示（不落库） */
    @PostMapping(value = "/collect/resume/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CollectStepPayload> parseResume(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "sessionId", required = false) String sessionId,
                                                  @RequestParam(value = "experienceVersion", required = false) String experienceVersion) {
        if (file == null || file.isEmpty()) {
            return Result.success(CollectStepPayload.builder().code(0).msg("请先上传简历文件").build());
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!isAllowedResumeFile(fileName)) {
            return Result.success(CollectStepPayload.builder().code(0).msg("仅支持 pdf/doc/docx 格式").build());
        }
        ResumeDocumentParser.ResumeParseText parsed;
        try {
            parsed = ResumeDocumentParser.parse(file, fileName);
        } catch (Exception e) {
            return Result.success(CollectStepPayload.builder().code(0).msg("简历内容提取失败，请检查文件").build());
        }
        if (parsed.fullText().isBlank()) {
            return Result.success(CollectStepPayload.builder().code(0).msg("简历内容为空，请更换文件后重试").build());
        }
        return Result.success(collectService.parseResumeProfile(
                sessionId, experienceVersion, fileName, parsed.textForLlm(), parsed.fullText()));
    }

    private boolean isAllowedResumeFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String n = fileName.trim().toLowerCase(Locale.ROOT);
        return n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx");
    }

    @GetMapping("/workflow/status")
    public Result<ChatPayload> workflowStatus(@RequestParam String sessionId) {
        return Result.success(assessmentService.getWorkflowStatus(sessionId));
    }

    /** 阶段性综合报告：scope=resume|skill|career|full */
    @GetMapping("/report/partial")
    public Result<CareerAssessmentVO> partialReport(@RequestParam String sessionId,
                                                    @RequestParam String scope) {
        return Result.success(assessmentService.getPartialReport(sessionId, scope));
    }

    /** 霍兰德兴趣测试完整报告 */
    @GetMapping("/report/holland")
    public Result<com.coze.aicoze.vo.HollandAnalysisVO> hollandReport(@RequestParam String sessionId) {
        return Result.success(assessmentService.getHollandReport(sessionId));
    }

    @Idempotent
    @PostMapping("/start-skill")
    public Result<ChatPayload> startSkill(@RequestBody AnswerRequest request) {
        return Result.success(assessmentService.startSkillAssessment(request.getSessionId()));
    }

    @Idempotent(required = false)
    @PostMapping("/send")
    public Result<ChatPayload> sendMessage(@RequestBody ChatMessageRequest request){
        ChatPayload aiMessage = assessmentService.chat(request.getSessionId(), request.getMessage());
        return Result.success(aiMessage);
    }

    @Idempotent
    @PostMapping("/skill/answer")
    public Result<ChatPayload> submitSkillAnswer(@RequestBody AnswerRequest request) {
        ChatPayload result = assessmentService.submitSkillAnswer(request.getSessionId(), request.getAnswer());
        return Result.success(result);
    }

    @Idempotent
    @PostMapping("/career/answer")
    public Result<ChatPayload> submitCareerAnswer(@RequestBody AnswerRequest request) {
        ChatPayload result = assessmentService.submitCareerAnswer(request.getSessionId(), request.getAnswer());
        return Result.success(result);
    }

    @Idempotent
    @PostMapping("/feedback")
    public Result<String> submitFeedback(@RequestBody FeedbackRequest request) {
        assessmentService.submitFeedback(request);
        return Result.success("反馈提交成功");
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String sessionId, @RequestParam String userMessage) {
        return Flux.create(sink -> {
            try {
                chatService.streamChat(sessionId, userMessage,
                        content -> {
                            // 直接发送内容，不包装 Result，前端更容易解析
                            if (content != null && !content.isEmpty()) {
                                sink.next(ServerSentEvent.builder(content)
                                        .event("message")
                                        .build());
                            }
                        },
                        usage -> {
                            // 发送结束信号
                            sink.next(ServerSentEvent.builder("[DONE]")
                                    .event("complete")
                                    .build());
                            sink.complete();
                        }
                );
            } catch (BusinessException e) {
                sink.next(ServerSentEvent.builder(SseErrorJson.format(e.getCode(), e.getMessage()))
                        .event("error")
                        .build());
                sink.complete();
            } catch (Exception e) {
                log.warn("streamChat failed, sessionId={}", sessionId, e);
                sink.next(ServerSentEvent.builder(SseErrorJson.format(BizErrorCodes.UNKNOWN_SYSTEM, "服务暂时异常，请稍后重试"))
                        .event("error")
                        .build());
                sink.complete();
            }
        });
    }
}














