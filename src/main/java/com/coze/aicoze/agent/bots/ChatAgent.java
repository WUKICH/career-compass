package com.coze.aicoze.agent.bots;

import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.dto.AssociationQuestionDTO;
import com.coze.aicoze.enums.AgentType;
import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.full.FullFlowRegistry;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.workflow.support.WorkflowLifecycleService;
import com.coze.openapi.client.chat.model.ChatUsage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatAgent extends BaseChatAgent {

    @Value("${agent.platform.chat:${agent.platform.default:coze}}")
    private String platform;
    @Value("${coze.chat-client.bot-id:}")
    private String cozeBotId;
    @Value("${tencent.lke.chat-client.app-key:}")
    private String tencentBotId;
    @Resource
    private AssociationAgent associationAgent;
    @Autowired
    private ContextManager contextManager;
    @Resource
    private PromptFactory promptFactory;
    @Resource
    private WorkflowLifecycleService workflowLifecycleService;
    @Autowired(required = false)
    private FullFlowRegistry fullFlowRegistry;

    @Override
    protected AgentType getAgentType() {
        return AgentType.CHAT;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    @Override
    public ChatResult doChat(String userMessage, String sessionId) {
        ChatResult chatResult = chat(userMessage, resolveBotId(), sessionId);
        // 完全版主聊仍调用 AssociationAgent；体验版 Workflow 下 presetOptions 为空为预期
        boolean fullSession = fullFlowRegistry != null && fullFlowRegistry.isFull(sessionId);
        if (fullSession
                && contextManager.getMode(sessionId) == AgentType.CHAT
                && StrUtil.isNotBlank(chatResult.getContent())) {
            String associationInput = promptFactory.buildAssociationUserMessage(chatResult.getContent());
            ChatResult associationResult = associationAgent.doChat(associationInput, sessionId);
            AssociationQuestionDTO associationQuestionDTO =
                    JsonParseUtil.toBeanQuiet(associationResult.getContent(), AssociationQuestionDTO.class);
            if (associationQuestionDTO == null) {
                log.error("association parse failed, sessionId={}", sessionId);
            } else {
                chatResult.setPresetOptions(normalizePresetOptions(associationQuestionDTO.getRelevantAnswer()));
            }
        }
        return chatResult;
    }

    /**
     * 去掉空项与过短碎片，限制条数与单条长度，避免前端一键发送无法通过语义闸门。
     */
    private List<String> normalizePresetOptions(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : raw) {
            if (StrUtil.isBlank(s)) {
                continue;
            }
            String t = s.strip();
            if (t.length() < 4) {
                continue;
            }
            if (t.length() > 500) {
                t = t.substring(0, 500).strip();
            }
            set.add(t);
            if (set.size() >= 4) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        chatStream(userMessage, resolveBotId(), sessionId, onContent, onComplete);
    }

    private String resolveBotId() {
        return "coze".equalsIgnoreCase(platform) ? cozeBotId : tencentBotId;
    }
}
