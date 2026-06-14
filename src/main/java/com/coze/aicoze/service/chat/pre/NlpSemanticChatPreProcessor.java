package com.coze.aicoze.service.chat.pre;

import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.service.chat.SemanticAnalysisService;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 语义相关性闸门：沿用原 {@code semanticAnalysis} 行为，作为前置链上的一环。
 * 后续可在同链上增加更小 {@link #getOrder()} 的本地过滤器（如违禁词）。
 */
@Component
@RequiredArgsConstructor
public class NlpSemanticChatPreProcessor implements ChatPreProcessor {

    /**
     * 本地规则类插件建议使用 100～500；NLP 靠后以降低无效模型调用成本。
     */
    public static final int ORDER_NLP_SEMANTIC = 1000;

    private final SemanticAnalysisService semanticAnalysisService;
    private final WorkflowLifecycleService workflowLifecycleService;

    @Override
    public int getOrder() {
        return ORDER_NLP_SEMANTIC;
    }

    @Override
    public Optional<ChatPayload> process(ChatPreProcessContext context) {
        if (workflowLifecycleService.isEnabled()) {
            return Optional.empty();
        }
        // 语义闸门：仅当判定“需要拦截/澄清”时返回消息；否则交给后续路由继续执行主链路。
        String blockReply = semanticAnalysisService.analyze(
                context.sessionId(),
                context.agentQuestionSnapshot(),
                context.userMessage());
        if (blockReply != null) {
            return Optional.of(ChatPayload.builder().content(blockReply).build());
        }
        return Optional.empty();
    }
}
