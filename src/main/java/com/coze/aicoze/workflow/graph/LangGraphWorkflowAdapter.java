package com.coze.aicoze.workflow.graph;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.FlowVariant;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * LangGraph4j 图结构对应的 transition 校验适配器。
 */
@Component
public class LangGraphWorkflowAdapter {

    public void assertTransitionAllowed(SessionWorkflowState state, FlowPhase from, FlowPhase to) {
        if (from == to) {
            return;
        }
        Set<String> edges = state.getVariant() == FlowVariant.FULL
                ? AssessmentWorkflowGraphBuilder.fullEdges()
                : AssessmentWorkflowGraphBuilder.trialEdges();
        String key = from.name() + "->" + to.name();
        if (!edges.contains(key)) {
            throw new BusinessException(BizErrorCodes.INVALID_WORKFLOW_PHASE,
                    "非法阶段迁移: " + key + ", variant=" + state.getVariant());
        }
    }
}
