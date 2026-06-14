package com.coze.aicoze.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.*;
import com.coze.aicoze.agent.bots.CareerTestAgent;
import com.coze.aicoze.agent.bots.ChatAgent;
import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.full.FullChatPayload;
import com.coze.aicoze.entity.User;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.full.FullAssessmentStore;
import com.coze.aicoze.full.FullFlowRegistry;
import com.coze.aicoze.full.flow.FullRoundHollandAgent;
import com.coze.aicoze.full.flow.FullRoundSkillAgent;
import com.coze.aicoze.infrastructure.knowledgegraph.service.KnowledgeGraphTalentSyncService;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.manager.RouterManager;
import com.coze.aicoze.mapper.UsersMapper;
import com.coze.aicoze.service.FullAssessmentService;
import com.coze.aicoze.service.chat.pre.ChatPreProcessChain;
import com.coze.aicoze.service.chat.pre.ChatPreProcessContext;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.component.AssessmentSessionService;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import com.coze.aicoze.workflow.support.WorkflowTransitionResolver;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.util.AdjustedStoragePayload;
import com.coze.aicoze.util.FullQuestionParser;
import com.coze.aicoze.util.JsonInputNormalizer;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.JwtUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.util.WorkYearsCodec;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FullAssessmentServiceImpl implements FullAssessmentService {

    private static final String DEFAULT_FULL_VERSION_LABEL = "完全版";

    private final ChatAgent chatAgent;
    private final FullRoundSkillAgent fullRoundSkillAgent;
    private final CareerTestAgent careerTestAgent;
    private final FullRoundHollandAgent fullRoundHollandAgent;
    private final RouterManager routerManager;
    private final AssessmentStore assessmentStore;
    private final ContextManager contextManager;
    private final UsersMapper usersMapper;
    private final PromptFactory promptFactory;
    private final AssessmentPersistence persistence;
    private final JwtUtil jwtUtil;
    private final KnowledgeGraphTalentSyncService knowledgeGraphTalentSyncService;
    private final FullFlowRegistry fullFlowRegistry;
    private final FullAssessmentStore fullAssessmentStore;
    private final ChatPreProcessChain chatPreProcessChain;
    private final WorkflowLifecycleService workflowLifecycleService;
    private final WorkflowTransitionResolver workflowTransitionResolver;
    private final WorkflowEngine workflowEngine;
    private final AssessmentSessionService assessmentSessionService;

    private void assertFullSession(String sessionId) {
        if (!fullFlowRegistry.isFull(sessionId)) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "该会话未通过完全版入口初始化，请使用 POST /full/chat/init");
        }
    }

    private Integer initOrSaveUser(ChatInitRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setWorkYears(WorkYearsCodec.toDbValue(request.getWorkYears()));
        user.setTargetJob(request.getTargetJob());
        user.setSkill(request.getSkill());
        user.setExpectedCity(request.getExpectedCity());
        user.setExpectedSalary(request.getExpectedSalary());
        user.setResume(StrUtil.blankToDefault(request.getResume(), ""));
        if (Boolean.TRUE.equals(request.getResumeSubmitted())) {
            user.setResumeSubmitted(true);
            user.setResumeClaims(StrUtil.blankToDefault(request.getResumeClaims(), ""));
        } else {
            user.setResumeSubmitted(false);
            user.setResumeClaims(null);
        }
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        usersMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FullChatPayload initAndFirstChat(ChatInitRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            contextManager.clearCozeChatIds(request.getSessionId());
            log.info("Cleared all Coze chatIds (full init), sessionId: {}", request.getSessionId());
        }

        String sessionId = StrUtil.isNotBlank(request.getSessionId())
                ? request.getSessionId()
                : String.valueOf(System.currentTimeMillis());

        fullFlowRegistry.register(sessionId);

        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
        Integer userId = session.getUserId();
        if (userId == null) {
            userId = initOrSaveUser(request);
            session.setUserId(userId);
        }
        assessmentSessionService.createOrRefresh(sessionId, userId,
                StrUtil.isNotBlank(request.getExperienceVersion())
                        ? request.getExperienceVersion()
                        : DEFAULT_FULL_VERSION_LABEL);
        session.setName(request.getName());
        session.setExperienceVersion(StrUtil.isNotBlank(request.getExperienceVersion())
                ? request.getExperienceVersion()
                : DEFAULT_FULL_VERSION_LABEL);
        session.setWorkYears(WorkYearsCodec.toPromptText(request.getWorkYears()));
        session.setTargetJob(request.getTargetJob());
        session.setSkill(request.getSkill());
        session.setExpectedCity(request.getExpectedCity());
        session.setExpectedSalary(request.getExpectedSalary());
        session.setSkillQuestion(null);
        session.setCareerQuestion(null);

        contextManager.setMode(AgentType.CHAT, sessionId);
        contextManager.setSkillTriggered(sessionId, false);
        contextManager.setCareerTriggered(sessionId, false);
        contextManager.setAgentQuestion(sessionId, null);

        String welcomeContent = promptFactory.buildWelcomeMessage(
                session.getTargetJob(), session.getExpectedCity());
        session.setInitialContextSent(true);

        knowledgeGraphTalentSyncService.syncAfterProfileConfirm(userId, session);

        workflowLifecycleService.onFullInitConfirmed(sessionId, userId, request);

        String token = jwtUtil.generateToken(userId, request.getName());

        return withPhase(FullChatPayload.builder()
                .sessionId(sessionId)
                .content(welcomeContent)
                .skillTestReady(false)
                .careerTestReady(false)
                .token(token)
                .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                .build(), sessionId);
    }

    private FullChatPayload withPhase(FullChatPayload payload, String sessionId) {
        if (payload == null) {
            return null;
        }
        payload.setPhase(workflowEngine.getState(sessionId).getPhase().name());
        return payload;
    }

    @Override
    public FullChatPayload chat(String sessionId, String message) {
        assertFullSession(sessionId);
        log.info("[full] chat sessionId={}, message.len={}", sessionId, message == null ? 0 : message.length());

        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
        workflowLifecycleService.assertMainChat(sessionId);

        String payload = session.isInitialContextSent()
                ? message
                : promptFactory.buildFirstMessage(
                        session.getExperienceVersion(),
                        session.getWorkYears(),
                        session.getSkill(),
                        session.getTargetJob(),
                        session.getExpectedCity(),
                        session.getExpectedSalary()) + "\n用户消息：" + message;
        session.setInitialContextSent(true);

        var preCtx = new ChatPreProcessContext(sessionId, message, contextManager.getAgentQuestion(sessionId));
        var blocked = chatPreProcessChain.run(preCtx);
        if (blocked.isPresent()) {
            return FullChatPayload.builder()
                    .content(blocked.get().getContent())
                    .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                    .build();
        }

        ChatResult chatResult = routerManager.route(sessionId).doChat(payload, sessionId);
        workflowLifecycleService.afterMainChatTurn(sessionId);

        var fullSkillDto = session.getSkillQuestion() != null
                ? FullQuestionParser.parseSkillQuestions(session.getSkillQuestion())
                : null;
        var fullCareerDto = session.getCareerQuestion() != null
                ? FullQuestionParser.parseCareerQuestions(session.getCareerQuestion())
                : null;

        FlowPhase phase = workflowEngine.getState(sessionId).getPhase();
        return withPhase(FullChatPayload.builder()
                .content(chatResult.getContent())
                .skillTestReady(phase == FlowPhase.SKILL_ACTIVE && fullSkillDto != null)
                .careerTestReady(phase == FlowPhase.CAREER_ACTIVE && fullCareerDto != null)
                .presetOptions(chatResult.getPresetOptions())
                .fullSkillTestQuestions(fullSkillDto)
                .fullCareerTestQuestions(fullCareerDto)
                .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                .build(), sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FullChatPayload submitSkillAnswer(String sessionId, String answer) {
        assertFullSession(sessionId);
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        workflowLifecycleService.assertSkillSubmit(sessionId);

        Integer userId = session.getUserId();
        String skill = session.getSkill();
        String targetJob = session.getTargetJob();

        CompletableFuture<String> assessmentFuture = fullRoundSkillAgent.submitAnswer(sessionId, userId, answer);

        try {
            String skillResult = assessmentFuture.get();
            if (StrUtil.isEmpty(skillResult)) {
                log.error("技能测试评估结果未获取, sessionId:{}, userId:{}", sessionId, userId);
                throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
            }

            TransitionOutcome outcome = workflowTransitionResolver.resolveSkillOutcome(skillResult);
            boolean skillPhaseDone = outcome == TransitionOutcome.COMPLETE;

            if (!skillPhaseDone) {
                workflowEngine.continueSkillRound(sessionId);
                String jsonOnly = JsonInputNormalizer.normalizeJsonInput(skillResult);
                if (StrUtil.isBlank(jsonOnly)) {
                    jsonOnly = skillResult.trim();
                }
                session.setSkillQuestion(jsonOnly);
                persistence.saveSkill(userId, sessionId, jsonOnly, "SKILL_QUESTION");
                var skillDto = FullQuestionParser.parseSkillQuestions(jsonOnly);
                return withPhase(FullChatPayload.builder()
                        .sessionId(sessionId)
                        .content("请继续完成本阶段技能测试。")
                        .skillTestReady(true)
                        .careerTestReady(false)
                        .fullSkillTestQuestions(skillDto)
                        .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                        .build(), sessionId);
            }

            workflowLifecycleService.onSkillComplete(sessionId, skillResult);
            persistence.saveSkill(userId, sessionId, skillResult, "SKILL_RESULT");
            session.setSkillResult(skillResult);

            chatAgent.doChat("以下是技能测试结果：\n" + skillResult, sessionId);

            String careerMessage = "你好";
            ChatResult careerResult = careerTestAgent.doChat(careerMessage, sessionId);

            if (session.getCareerQuestion() != null) {
                persistence.saveCareer(userId, sessionId, session.getCareerQuestion(), "QUESTION");
            }

            String jsonForSkillReport = CozeJsonExtractor.extractLastJsonObject(skillResult);
            if (StrUtil.isBlank(jsonForSkillReport)) {
                jsonForSkillReport = skillResult.trim();
            }
            SkillAnalysisReportVO skillReport = JsonParseUtil.toBeanQuiet(jsonForSkillReport, SkillAnalysisReportVO.class);
            if (skillReport == null) {
                log.error("技能最终结果无法解析为报告 JSON，sessionId={}", sessionId);
                throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "技能报告解析失败，请稍后重试。");
            }
            skillReport.setName(session.getName());
            skillReport.setTargetJob(targetJob);
            skillReport.setSkill(skill);
            skillReport.setTestTime(LocalDateTime.now());
            persistence.updateSkillAdjustedAsync(sessionId, userId, AdjustedStoragePayload.fromReport(skillReport));

            knowledgeGraphTalentSyncService.syncAfterSkillTestComplete(userId, session);

            var careerDto = FullQuestionParser.parseCareerQuestions(session.getCareerQuestion());
            return withPhase(FullChatPayload.builder()
                    .sessionId(sessionId)
                    .content(careerResult.getContent())
                    .skillTestReady(false)
                    .careerTestReady(careerDto != null)
                    .presetOptions(careerResult.getPresetOptions())
                    .fullCareerTestQuestions(careerDto)
                    .skillReport(skillReport)
                    .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                    .build(), sessionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("完全版技能结果同步错误，sessionId = {}", sessionId, e);
            throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FullChatPayload submitCareerAnswer(String sessionId, String answer) {
        assertFullSession(sessionId);
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        workflowLifecycleService.assertCareerSubmit(sessionId);

        Integer userId = session.getUserId();
        CompletableFuture<String> assessmentFuture = fullRoundHollandAgent.submitAnswer(sessionId, userId, answer);

        try {
            String careerResult = assessmentFuture.get();
            if (StrUtil.isEmpty(careerResult)) {
                log.error("职业测试评估结果未获取, sessionId:{}, userId:{}", sessionId, userId);
                throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
            }

            TransitionOutcome outcome = workflowTransitionResolver.resolveCareerOutcome(careerResult);
            boolean hollandPhaseDone = outcome == TransitionOutcome.COMPLETE;

            if (!hollandPhaseDone) {
                session.setCareerQuestion(careerResult);
                persistence.saveCareer(userId, sessionId, careerResult, "QUESTION");
                var careerDto = FullQuestionParser.parseCareerQuestions(careerResult);
                return withPhase(FullChatPayload.builder()
                        .sessionId(sessionId)
                        .content("请继续完成霍兰德测试。")
                        .skillTestReady(false)
                        .careerTestReady(careerDto != null)
                        .fullCareerTestQuestions(careerDto)
                        .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                        .build(), sessionId);
            }

            workflowLifecycleService.onCareerComplete(sessionId, careerResult);
            persistence.saveCareer(userId, sessionId, careerResult, "MBIT_RESULT");

            String chatPrompt = "以下是霍兰德测试评估结果：\n" + careerResult;
            ChatResult chatResult = chatAgent.doChat(chatPrompt, sessionId);
            session.setCareerResult(careerResult);

            String jsonForHolland = CozeJsonExtractor.extractLastJsonObject(careerResult);
            if (StrUtil.isBlank(jsonForHolland)) {
                jsonForHolland = careerResult.trim();
            }
            HollandAnalysisVO hollandReport = JsonParseUtil.toBeanQuiet(jsonForHolland, HollandAnalysisVO.class);
            if (hollandReport == null) {
                log.error("霍兰德最终结果无法解析为报告 JSON，sessionId={}", sessionId);
                throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "霍兰德报告解析失败，请稍后重试。");
            }
            hollandReport.setName(session.getName());
            hollandReport.setTargetJob(session.getTargetJob());
            hollandReport.setTestTime(LocalDateTime.now());

            if (StrUtil.isNotBlank(hollandReport.getHollandCode())) {
                persistence.updateHollandCodeAsync(sessionId, userId, hollandReport.getHollandCode());
            }

            knowledgeGraphTalentSyncService.syncAfterHollandReport(userId, session, hollandReport);

            workflowEngine.markReportReady(sessionId);
            return withPhase(FullChatPayload.builder()
                    .sessionId(sessionId)
                    .content(chatResult.getContent())
                    .skillTestReady(false)
                    .careerTestReady(false)
                    .hollandReport(hollandReport)
                    .skillTestRound(fullAssessmentStore.getSkillRound(sessionId))
                    .build(), sessionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("完全版职业测试处理失败, sessionId={}", sessionId, e);
            throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
        }
    }

}
