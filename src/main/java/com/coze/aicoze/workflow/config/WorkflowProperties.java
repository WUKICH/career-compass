package com.coze.aicoze.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.workflow")
public class WorkflowProperties {

    private Intent intent = new Intent();
    private State state = new State();
    private MainChat mainChat = new MainChat();

    @Data
    public static class Intent {
        private double readySkillConfidence = 0.75;
        private int minMainChatTurns = 2;
    }

    @Data
    public static class State {
        private String keyPrefix = "wf:state:v1:";
        private long ttlSeconds = 86400;
    }

    @Data
    public static class MainChat {
        /** 最短有效长度（含中文短答如「好的」） */
        private int minLength = 2;
        /** 无中文时拉丁字符最短长度，低于此视为乱输（如 kkk、loo） */
        private int minLatinLength = 5;
        private String shortInputReply;
        private String gibberishReply;
        /** 是否在通过本地规则后调用语义分析（替代 legacy NlpSemanticChatPreProcessor） */
        private boolean semanticGateEnabled = true;
        /** 确认后首轮主聊回复最大字符数，超出则软截断 */
        private int maxFirstReplyChars = 220;
    }
}
