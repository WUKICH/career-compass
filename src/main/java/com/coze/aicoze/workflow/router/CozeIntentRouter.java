package com.coze.aicoze.workflow.router;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.coze.aicoze.agent.platform.PlatformChatResponse;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.workflow.invoker.CozeBotInvoker;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import com.coze.aicoze.workflow.validation.BotOutputValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CozeIntentRouter implements IntentRouter {

    private final CozeBotInvoker cozeBotInvoker;
    private final BotOutputValidator botOutputValidator;

    @Value("${agent.platform.nlp:${agent.platform.default:coze}}")
    private String nlpPlatform;

    @Value("${coze.NlP-client.bot-id:}")
    private String cozeNlpBotId;

    @Override
    public IntentRouteResult classify(SessionWorkflowState state, String userMessage, String recentQuestionSummary) {
        String prompt = buildRouterPrompt(state, userMessage, recentQuestionSummary);
        try {
            PlatformChatResponse response = cozeBotInvoker.invokeStateless(
                    prompt,
                    cozeNlpBotId,
                    state.getSessionId() + ":router:" + System.nanoTime(),
                    AgentType.NLP_START,
                    nlpPlatform);
            String raw = response == null ? null : response.getContent();
            return parse(raw);
        } catch (Exception e) {
            log.warn("IntentRouter failed, sessionId={}, default CONTINUE", state.getSessionId(), e);
            return IntentRouteResult.builder()
                    .intent("CONTINUE_CONSULTATION")
                    .confidence(0.0)
                    .readyForSkill(false)
                    .offTopic(false)
                    .build();
        }
    }

    private String buildRouterPrompt(SessionWorkflowState state, String userMessage, String recentQuestionSummary) {
        return String.format("""
                【任务】判断用户意图，只输出 JSON（intent-router.schema）。
                【上下文】phase=%s, mainChatTurns=%d
                【助手最近问题摘要】%s
                【用户消息】%s
                """,
                state.getPhase(),
                state.getMainChatTurnCount(),
                StrUtil.blankToDefault(recentQuestionSummary, "无"),
                userMessage);
    }

    private IntentRouteResult parse(String raw) {
        if (StrUtil.isBlank(raw)) {
            return failOpen();
        }
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        if (json != null && !botOutputValidator.validate("intent-router.schema.json", json)) {
            log.debug("Intent JSON schema validation failed, fail-open");
            return failOpen();
        }
        JSONObject obj = JsonParseUtil.parseObjSanitizedQuiet(json != null ? json : raw);
        if (obj == null) {
            return failOpen();
        }
        String intent = JsonParseUtil.getStr(obj, "intent", "意图");
        if (intent == null) {
            intent = "CONTINUE_CONSULTATION";
        }
        double confidence = 0.0;
        Object score = obj.get("confidence");
        if (score instanceof Number n) {
            confidence = n.doubleValue();
        }
        String reply = JsonParseUtil.getStr(obj, "reply", "回复");
        boolean ready = "READY_FOR_SKILL_ASSESSMENT".equalsIgnoreCase(intent.trim());
        boolean offTopic = "OFF_TOPIC".equalsIgnoreCase(intent.trim());
        return IntentRouteResult.builder()
                .intent(intent)
                .confidence(confidence)
                .reply(reply)
                .readyForSkill(ready)
                .offTopic(offTopic)
                .build();
    }

    private IntentRouteResult failOpen() {
        return IntentRouteResult.builder()
                .intent("CONTINUE_CONSULTATION")
                .confidence(0.0)
                .readyForSkill(false)
                .offTopic(false)
                .build();
    }
}
