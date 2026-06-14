package com.coze.aicoze.vo;

import com.coze.aicoze.dto.ChatInitRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CollectStepPayload {
    /** 0 校验不通过不进入下一题，1 通过 */
    private Integer code;
    /** 校验失败时的提示信息 */
    private String msg;
    /** 下一题文案（code=1 且未结束时返回） */
    private String nextQuestion;
    /** 下一题 key（code=1 且未结束时返回） */
    private String nextQuestionKey;
    /** 下一题的预选项（可选） */
    private List<String> presetOptions;
    /**
     * 会话 id，收集阶段和正式对话阶段统一使用。
     * 收集阶段每一步请求都带上，完成后继续用于正式对话。
     */
    private String sessionId;
    /** 收集完成后的 JWT token */
    private String token;
    /** 收集完成后的首条 AI 回复（可选，用于直接展示） */
    private String content;
    /** 当前 workflow 阶段（确认落库后返回，供前端展示「开始能力测评」等） */
    private String phase;
    /** 是否展示「开始能力测评」入口 */
    private Boolean suggestStartSkill;
    /** 技能测评题目是否已预生成就绪 */
    private Boolean skillQuestionsPrepared;
    /**
     * 仅 {@code /chat/collect/preview-skill}：NLP + 收集 Agent 返回的结构化画像，供确认页展示（不落库）。
     */
    private ChatInitRequest profilePreview;
}
