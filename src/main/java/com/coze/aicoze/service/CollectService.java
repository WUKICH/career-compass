package com.coze.aicoze.service;

import com.coze.aicoze.dto.CollectConfirmRequest;
import com.coze.aicoze.dto.CollectSkillPreviewRequest;
import com.coze.aicoze.dto.CollectStepRequest;
import com.coze.aicoze.vo.CollectStepPayload;

public interface CollectService {

    CollectStepPayload collectStep(CollectStepRequest request);

    CollectStepPayload confirmCollect(CollectConfirmRequest request);

    CollectStepPayload previewSkillProfile(CollectSkillPreviewRequest request);

    /**
     * @param resumeTextForLlm 送入简历解析 Agent 的文本（可能经分节预算截断）
     * @param resumeTextFull   完整提取文本，用于落库 resumeClaims；为空时回退为 forLlm 文本
     */
    CollectStepPayload parseResumeProfile(String sessionId, String experienceVersion, String resumeFileName,
                                          String resumeTextForLlm, String resumeTextFull);
}
