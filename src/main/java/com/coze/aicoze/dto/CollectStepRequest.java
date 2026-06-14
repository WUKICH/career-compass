package com.coze.aicoze.dto;

import lombok.Data;

@Data
public class CollectStepRequest {
    /**
     * 会话 id，收集阶段和正式对话阶段统一使用。
     * 首次请求可为空，后续用后端返回的 sessionId。
     */
    private String sessionId;
    /** 当前问题标识，与后端 COLLECT_STEPS 顺序一致：experienceVersion, welcome, name, workYears, targetJob, expectedCity, expectedSalary, skill */
    private String questionKey;
    /** 用户原始输入 */
    private String userInput;
    /**
     * 为 true 时仅返回首步题目与会话 id，不校验、不写入答案（用于前端进入收集流程时拉取后端话术）。
     */
    private Boolean bootstrap;
}
