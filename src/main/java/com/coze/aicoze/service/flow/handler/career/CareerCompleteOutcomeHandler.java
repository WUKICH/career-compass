package com.coze.aicoze.service.flow.handler.career;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.infrastructure.knowledgegraph.service.KnowledgeGraphTalentSyncService;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.service.component.AssessmentPersistence;
import com.coze.aicoze.service.flow.CareerSubmitContext;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.service.flow.handler.CareerSubmitOutcomeHandler;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.workflow.support.PostCareerWorkflowService;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareerCompleteOutcomeHandler implements CareerSubmitOutcomeHandler {
    private final AssessmentPersistence persistence;
    private final ContextManager contextManager;
    private final KnowledgeGraphTalentSyncService knowledgeGraphTalentSyncService;
    private final WorkflowLifecycleService workflowLifecycleService;
    private final PostCareerWorkflowService postCareerWorkflowService;

    @Override
    public TransitionOutcome outcome() {
        return TransitionOutcome.COMPLETE;
    }

    @Override
    public ChatPayload handle(CareerSubmitContext context) {
        persistence.saveCareer(context.userId(), context.sessionId(), context.careerResult(), "MBIT_RESULT");
        context.session().setCareerResult(context.careerResult());
        contextManager.setMode(com.coze.aicoze.enums.AgentType.CHAT, context.sessionId());

        String jsonForHolland = CozeJsonExtractor.extractLastJsonObject(context.careerResult());
        if (StrUtil.isBlank(jsonForHolland)) {
            jsonForHolland = context.careerResult().trim();
        }
        HollandAnalysisVO hollandReport = JsonParseUtil.toBeanQuiet(jsonForHolland, HollandAnalysisVO.class);
        if (hollandReport == null) {
            log.error("霍兰德最终结果无法解析为报告 JSON，sessionId={}", context.sessionId());
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "霍兰德报告解析失败，请稍后重试。");
        }
        hollandReport.setName(context.session().getName());
        hollandReport.setTargetJob(context.session().getTargetJob());
        hollandReport.setTestTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(hollandReport.getHollandCode())) {
            persistence.updateHollandCodeAsync(context.sessionId(), context.userId(), hollandReport.getHollandCode());
        }
        knowledgeGraphTalentSyncService.syncAfterHollandReport(context.userId(), context.session(), hollandReport);

        if (workflowLifecycleService.isEnabled()) {
            return postCareerWorkflowService.startPostCareerFlow(
                    context.sessionId(), context.session(), hollandReport, jsonForHolland);
        }

        return ChatPayload.builder()
                .sessionId(context.sessionId())
                .content("职业兴趣测评已完成。")
                .hollandReport(hollandReport)
                .skillTestReady(false)
                .careerTestReady(false)
                .build();
    }
}
