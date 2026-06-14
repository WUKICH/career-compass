package com.coze.aicoze.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.*;
import com.coze.aicoze.agent.bots.CareerTestAgent;
import com.coze.aicoze.agent.bots.ChatAgent;
import com.coze.aicoze.agent.bots.SkillTestAgent;
import com.coze.aicoze.dto.*;
import com.coze.aicoze.entity.User;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.manager.RouterManager;
import com.coze.aicoze.mapper.UsersMapper;
import com.coze.aicoze.infrastructure.knowledgegraph.service.KnowledgeGraphTalentSyncService;
import com.coze.aicoze.full.FullAssessmentStore;
import com.coze.aicoze.full.FullFlowRegistry;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.component.AssessmentSessionService;
import com.coze.aicoze.service.flow.CareerSubmitContext;
import com.coze.aicoze.service.AssessmentService;
import com.coze.aicoze.service.flow.SkillSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.service.chat.SemanticAnalysisService;
import com.coze.aicoze.service.flow.handler.CareerSubmitOutcomeHandlerRegistry;
import com.coze.aicoze.service.flow.handler.SkillSubmitOutcomeHandlerRegistry;
import com.coze.aicoze.util.JwtUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.util.WorkYearsCodec;
import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.dto.experience.SkillTestQuestionDto;
import com.coze.aicoze.util.ExperienceQuestionParser;
import com.coze.aicoze.vo.CareerAssessmentVO;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.workflow.composer.ReportComposer;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.ReportScope;
import com.coze.aicoze.workflow.service.WorkflowStartSkillService;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.aicoze.workflow.support.MainChatReplyTrimmer;
import com.coze.aicoze.workflow.support.PostCareerWorkflowService;
import com.coze.aicoze.workflow.support.QuestionPreparationService;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import com.coze.aicoze.workflow.support.WorkflowTransitionResolver;
import com.coze.aicoze.workflow.validation.BotOutputValidator;
import com.coze.aicoze.util.CozeJsonExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentServiceImpl implements AssessmentService {
    private final ChatAgent chatAgent;
    private final SkillTestAgent skillTestAgent;
    private final CareerTestAgent careerTestAgent;
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
    private final SkillSubmitOutcomeHandlerRegistry skillSubmitOutcomeHandlerRegistry;
    private final CareerSubmitOutcomeHandlerRegistry careerSubmitOutcomeHandlerRegistry;
    private final SemanticAnalysisService semanticAnalysisService;
    private final WorkflowLifecycleService workflowLifecycleService;
    private final WorkflowTransitionResolver workflowTransitionResolver;
    private final QuestionPreparationService questionPreparationService;
    private final WorkflowStartSkillService workflowStartSkillService;
    private final BotOutputValidator botOutputValidator;
    private final WorkflowProperties workflowProperties;
    private final WorkflowEngine workflowEngine;
    private final PostCareerWorkflowService postCareerWorkflowService;
    private final ReportComposer reportComposer;
    private final AssessmentSessionService assessmentSessionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatPayload initAndFirstChat(ChatInitRequest request) {
        // 1. 如果是更新个人信息（请求中包含 sessionId），清空所有 Coze chatId
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            contextManager.clearCozeChatIds(request.getSessionId());
            log.info("Cleared all Coze chatIds due to user info update, old sessionId: {}", request.getSessionId());
        }
        
        // 2. 生成或使用 sessionId（收集阶段可能已经生成并传入）
        String sessionId = StrUtil.isNotBlank(request.getSessionId())
                ? request.getSessionId()
                : String.valueOf(System.currentTimeMillis());
        // 3. 初始化会话并落库用户（仅在此处写入数据库，收集阶段不落库）
        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
        Integer userId = session.getUserId();
        if (userId == null) {
            userId = initOrSaveUser(request);
            session.setUserId(userId);
        }
        assessmentSessionService.createOrRefresh(sessionId, userId, request.getExperienceVersion());
        session.setName(request.getName());
        session.setExperienceVersion(request.getExperienceVersion());
        session.setWorkYears(WorkYearsCodec.toPromptText(request.getWorkYears()));
        session.setTargetJob(request.getTargetJob());
        session.setSkill(request.getSkill());
        session.setExpectedCity(request.getExpectedCity());
        session.setExpectedSalary(request.getExpectedSalary());
        session.setSkillQuestion(null);
        session.setCareerQuestion(null);
        // 重置本地状态机锁：初始化新的对话流程（同一 sessionId 重新开始时也应允许）。
        contextManager.setMode(AgentType.CHAT, sessionId);
        contextManager.setSkillTriggered(sessionId, false);
        contextManager.setCareerTriggered(sessionId, false);
        contextManager.setAgentQuestion(sessionId, null);

        // 3. 异步生成技能/职业测试题目(待定)

        // 4. 体验版首轮：固定短欢迎语 + 引导技能测评（不调主聊 Bot，避免冗长问卷式回复）
        session.setInitialContextSent(true);

        knowledgeGraphTalentSyncService.syncAfterProfileConfirm(userId, session);

        workflowLifecycleService.onInitConfirmed(sessionId, userId, request);
        questionPreparationService.prepareAfterConfirm(sessionId);

        // 5. 生成JWT Token
        String token = jwtUtil.generateToken(userId, request.getName());

        String firstReply = MainChatReplyTrimmer.trimIfNeeded(
                promptFactory.buildTrialFirstReply(request.getTargetJob(), request.getExpectedCity()),
                workflowProperties.getMainChat().getMaxFirstReplyChars());

        ChatPayload payload = ChatPayload.builder()
                .sessionId(sessionId)
                .content(firstReply)
                .skillTestReady(false)
                .careerTestReady(false)
                .token(token)
                .build();
        return workflowLifecycleService.enrich(payload, sessionId);
    }


    @Override
    public ChatPayload startSkillAssessment(String sessionId) {
        return workflowStartSkillService.startSkill(sessionId);
    }

    @Override
    public ChatPayload getWorkflowStatus(String sessionId) {
        return workflowLifecycleService.getWorkflowStatus(sessionId);
    }


    @Override
    public ChatPayload chat(String sessionId, String message) {
        AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);

        if (workflowEngine.getState(sessionId).getPhase()
                == com.coze.aicoze.workflow.model.FlowPhase.CONSENSUS_ACTIVE) {
            return postCareerWorkflowService.handleConsensusMessage(sessionId, message, session);
        }

        workflowLifecycleService.assertMainChat(sessionId);

        var intentBlocked = workflowLifecycleService.preMainChat(
                sessionId, message, contextManager.getAgentQuestion(sessionId));
        if (intentBlocked.isPresent()) {
            return intentBlocked.get();
        }

        String payload = workflowLifecycleService.prependAnchor(sessionId, message, session);
        session.setInitialContextSent(true);

        ChatResult chatResult = routerManager.route(sessionId).doChat(payload, sessionId);
        workflowLifecycleService.afterMainChatTurn(sessionId);
        SkillTestQuestionDto skillTestQuestionDto = null;
        CareerTestQuestionDto careerTestQuestionDto = null;

        if (session.getSkillQuestion() != null) {
            skillTestQuestionDto = ExperienceQuestionParser.parseSkillQuestions(session.getSkillQuestion());
        }
        if (session.getCareerQuestion() != null) {
            careerTestQuestionDto = ExperienceQuestionParser.parseCareerQuestions(session.getCareerQuestion());
        }

        ChatPayload result = ChatPayload.builder()
                .content(chatResult.getContent())
                .skillTestReady(false)
                .careerTestReady(false)
                .presetOptions(chatResult.getPresetOptions())
                .skillTestQuestions(skillTestQuestionDto)
                .careerTestQuestions(careerTestQuestionDto)
                .build();
        return workflowLifecycleService.enrich(result, sessionId);
    }

    //技能测试提交答案后
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatPayload submitSkillAnswer(String sessionId, String answer){
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        workflowLifecycleService.assertSkillSubmit(sessionId);
        
        // 在异步任务执行前提取需要的数据，避免在异步线程中访问 session-scoped bean
        Integer userId = session.getUserId();
        String skill = session.getSkill();
        String targetJob = session.getTargetJob();
        
        // 异步评估：将“评估”与“编排”解耦，避免阻塞主链路太久；拿到结果后再交给状态机分支处理。
        CompletableFuture<String> assessmentFuture = skillTestAgent.skillAssessmentAsync(sessionId,userId,answer);

        // 结果编排：根据状态机转移结果选择不同的 outcome handler（继续对话/完成阶段/触发下一阶段等）。
        try {
            String skillResult = assessmentFuture.get();
            if (StrUtil.isEmpty(skillResult)){
                log.error("技能测试评估结果未获取, sessionId:{}, userId:{}", sessionId, userId);
                throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
            }
            log.info("技能测试评估结果已获取, sessionId={}, 当前模型：{}", sessionId, contextManager.getMode(sessionId));
            validateGradeOutputQuiet("skill-grade.schema.json", skillResult);

            TransitionOutcome transition = resolveSkillTransition(sessionId, skillResult);
            SkillSubmitContext submitContext = new SkillSubmitContext(sessionId, session, userId, skillResult, skill, targetJob);
            ChatPayload payload = skillSubmitOutcomeHandlerRegistry.get(transition).handle(submitContext);
            return workflowLifecycleService.enrich(payload, sessionId);
        } catch (Exception e) {
            log.error("技能结果同步到chatAgent错误，sessionId = {}", sessionId, e);
            throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
        }

    }

    //mbti提交答案后
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatPayload submitCareerAnswer(String sessionId, String answer) {
        AssessmentStore.SessionAssessment session = assessmentStore.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        workflowLifecycleService.assertCareerSubmit(sessionId);

        // 在异步任务执行前提取需要的数据，避免在异步线程中访问 session-scoped bean
        Integer userId = session.getUserId();
        
        // 异步评估：先拿到结构化/可解释的评估结果，再交由后端编排决定“下一步”。
        CompletableFuture<String> assessmentFuture = careerTestAgent.careerAssessmentAsync(sessionId,userId,answer);
        
        // 结果编排：同技能测评，走状态机转移表 + outcome handler 的统一分支处理。
        try {
            String careerResult = assessmentFuture.get(); // 等待异步任务完成
            if (StrUtil.isEmpty(careerResult)) {
                log.error("职业测试评估结果未获取, sessionId:{}, userId:{}", sessionId, userId);
                throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
            }
            log.info("职业测试评估结果已获取, sessionId={}, 当前模型：{}", sessionId, contextManager.getMode(sessionId));
            validateGradeOutputQuiet("career-grade.schema.json", careerResult);

            TransitionOutcome transition = resolveCareerTransition(sessionId, careerResult);
            CareerSubmitContext submitContext = new CareerSubmitContext(sessionId, session, userId, careerResult);
            ChatPayload payload = careerSubmitOutcomeHandlerRegistry.get(transition).handle(submitContext);
            return workflowLifecycleService.enrich(payload, sessionId);
        } catch (Exception e) {
            log.error("处理职业测试答案失败, sessionId={}", sessionId, e);
            // 如果出错，返回错误信息
            throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(FeedbackRequest feedbackRequest) {
        AssessmentStore.SessionAssessment session = assessmentStore.get(feedbackRequest.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        
        Integer userId = session.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不存在");
        }

        String feedbackContent = JSONUtil.toJsonPrettyStr(feedbackRequest.getFeedbackItems());
        persistence.saveFeedback(userId, feedbackRequest.getSessionId(), feedbackContent);
        log.info("评估反馈已保存, sessionId={}, userId={}, feedbackContent={}", feedbackRequest.getSessionId(), userId, feedbackContent);
        fullFlowRegistry.unregister(feedbackRequest.getSessionId());
        fullAssessmentStore.clear(feedbackRequest.getSessionId());
        workflowLifecycleService.onFeedback(feedbackRequest.getSessionId());
        //清理用户数据
        assessmentStore.clear(feedbackRequest.getSessionId());
    }

    @Override
    public String semanticAnalysis(String sessionId, String agentQuestion, String userAnswer) {
        return semanticAnalysisService.analyze(sessionId, agentQuestion, userAnswer);
    }

    /** 收集完成并校验通过后，将用户信息写入数据库（仅插入，收集阶段不落库） */
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

    private static Integer parseIdealSalaryK(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ignored) {
            Matcher m = Pattern.compile("(\\d+)").matcher(s);;
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
            return null;
        }
    }

    private void validateGradeOutputQuiet(String schemaFile, String raw) {
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        if (json != null && !botOutputValidator.validate(schemaFile, json)) {
            log.debug("Bot output schema validation failed (fail-open), schema={}, session fragment len={}",
                    schemaFile, json.length());
        }
    }

    private TransitionOutcome resolveSkillTransition(String sessionId, String skillResult) {
        return workflowTransitionResolver.resolveSkillOutcome(skillResult);
    }

    private TransitionOutcome resolveCareerTransition(String sessionId, String careerResult) {
        return workflowTransitionResolver.resolveCareerOutcome(careerResult);
    }

    @Override
    public CareerAssessmentVO getPartialReport(String sessionId, String scope) {
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "缺少 sessionId");
        }
        assessmentStore.init(sessionId);
        return reportComposer.composePartial(sessionId, ReportScope.parse(scope));
    }

    @Override
    public com.coze.aicoze.vo.HollandAnalysisVO getHollandReport(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "缺少 sessionId");
        }
        assessmentStore.init(sessionId);
        return reportComposer.composeHolland(sessionId);
    }
}