package com.coze.aicoze.manager;


import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.bots.CareerTestAgent;
import com.coze.aicoze.agent.bots.ChatAgent;
import com.coze.aicoze.agent.bots.SkillTestAgent;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.workflow.engine.WorkflowEngine;
import com.coze.aicoze.workflow.model.FlowPhase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class RouterManager {

    private static final Set<FlowPhase> MAIN_CHAT_PHASES = Set.of(
            FlowPhase.COLLECT,
            FlowPhase.CONFIRMED,
            FlowPhase.MAIN_CHAT,
            FlowPhase.REPORT_READY,
            FlowPhase.CLOSED
    );

    private final ChatAgent chatAgent;
    private final SkillTestAgent skillTestAgent;
    private final CareerTestAgent careerTestAgent;
    private final ContextManager contextManager;
    private final WorkflowEngine workflowEngine;
    private final Map<AgentType, BaseChatAgent> routeRegistry = new EnumMap<>(AgentType.class);

    @PostConstruct
    void initRouteRegistry() {
        routeRegistry.put(AgentType.SKILL_TEST, skillTestAgent);
        routeRegistry.put(AgentType.SKILL_END, careerTestAgent);
        routeRegistry.put(AgentType.CAREER_TEST, careerTestAgent);
    }

    /**
     * 根据会话当前状态选择 Agent；未命中则回退到主聊天 Agent。
     * Workflow 开启时：主聊阶段始终以 ChatAgent 处理 /chat/send，避免预生成题目误改 mode 导致路由到测评 Bot。
     */
    public BaseChatAgent route(String sessionId) {
        FlowPhase phase = workflowEngine.getState(sessionId).getPhase();
        if (MAIN_CHAT_PHASES.contains(phase)) {
            return chatAgent;
        }
        AgentType mode = contextManager.getMode(sessionId);
        return routeRegistry.getOrDefault(mode, chatAgent);
    }
}
