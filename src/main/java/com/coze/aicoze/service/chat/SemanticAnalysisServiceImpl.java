package com.coze.aicoze.service.chat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.agent.bots.NlpAgent;
import com.coze.aicoze.dto.SemanticAnalysisDTO;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.PromptFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticAnalysisServiceImpl implements SemanticAnalysisService {

    private final ContextManager contextManager;
    private final PromptFactory promptFactory;
    private final NlpAgent nlpAgent;

    /**
     * 只在“非常确信无关”时才拦截，避免误杀中文同义表达/合并回答。
     */
    private static final double BLOCK_SCORE_THRESHOLD = 0.80;

    /**
     * 最多澄清 1 次，避免用户被卡死反复试。
     */
    private static final int MAX_CLARIFY_TIMES = 1;

    @Override
    public String analyze(String sessionId, String agentQuestion, String userAnswer) {
        if (contextManager.getAgentQuestion(sessionId) == null) {
            contextManager.setAgentQuestion(sessionId, agentQuestion);
        }
        String prompt = promptFactory.buildSemanticAnalysisMessage(agentQuestion, userAnswer);
        ChatResult analysisResult = nlpAgent.doChat(prompt, sessionId);
        SemanticAnalysisDTO analysisDTO = parseSemanticAnalysisResult(analysisResult.getContent(), sessionId);

        Decision decision = decide(analysisDTO);
        if (decision == Decision.PASS) {
            contextManager.setAgentQuestion(sessionId, null);
            contextManager.resetSemanticClarifyCount(sessionId);
            return null;
        }

        // CLARIFY：最多追问一次；超过次数则放行，避免“卡死”
        if (decision == Decision.CLARIFY) {
            int c = contextManager.getSemanticClarifyCount(sessionId);
            if (c < MAX_CLARIFY_TIMES) {
                contextManager.incrementSemanticClarifyCount(sessionId);
                return StrUtil.isBlank(analysisDTO.getReply())
                        ? "我大概懂你的意思了～不过为了更准确一点，你能再贴近问题补充一句吗？"
                        : analysisDTO.getReply();
            }
            // 超过澄清次数：直接放行
            contextManager.setAgentQuestion(sessionId, null);
            contextManager.resetSemanticClarifyCount(sessionId);
            return null;
        }

        // BLOCK：只拦截“高置信度无关”；否则降级为澄清或放行
        if (decision == Decision.BLOCK) {
            // 这里的 BLOCK 已经过阈值判定；拦截时不清 question，允许用户重答
            return StrUtil.isBlank(analysisDTO.getReply())
                    ? "这条回复和问题核心不太对得上呢。你可以换个说法，围绕问题里的关键信息回答一下～"
                    : analysisDTO.getReply();
        }

        // 安全兜底：放行
        contextManager.setAgentQuestion(sessionId, null);
        contextManager.resetSemanticClarifyCount(sessionId);
        return null;
    }

    private enum Decision {
        PASS,
        CLARIFY,
        BLOCK
    }

    private Decision decide(SemanticAnalysisDTO dto) {
        if (dto == null) {
            return Decision.CLARIFY;
        }
        // 兼容旧协议：相关性=高 => 通过；相关性=低 => 澄清（不直接拦）
        if ("高".equals(dto.getRelevance())) {
            return Decision.PASS;
        }

        String verdict = dto.getVerdict();
        Double score = dto.getScore();
        if (verdict != null) {
            String v = verdict.trim().toUpperCase();
            if ("PASS".equals(v)) {
                return Decision.PASS;
            }
            if ("CLARIFY".equals(v)) {
                return Decision.CLARIFY;
            }
            if ("BLOCK".equals(v)) {
                // 只有高置信度才拦截，否则降级为澄清
                if (score != null && score >= BLOCK_SCORE_THRESHOLD) {
                    return Decision.BLOCK;
                }
                return Decision.CLARIFY;
            }
        }

        // 无 verdict：若 score 可用且很低/很高可以做更细策略；目前默认澄清一次即可
        return Decision.CLARIFY;
    }

    private SemanticAnalysisDTO parseSemanticAnalysisResult(String rawContent, String sessionId) {
        try {
            if (StrUtil.isBlank(rawContent)) {
                log.warn("语义分析返回为空，sessionId={}", sessionId);
                throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "语义分析暂不可用，请稍后再试。");
            }
            JSONObject jsonObject = JsonParseUtil.parseObjSanitizedQuiet(rawContent);
            if (jsonObject == null) {
                throw new IllegalArgumentException("语义分析结果不是合法 JSON");
            }
            SemanticAnalysisDTO dto = new SemanticAnalysisDTO();
            dto.setRelevance(JsonParseUtil.getStr(jsonObject, "相关性", "relevance"));
            dto.setReply(JsonParseUtil.getStr(jsonObject, "回复", "reply"));
            dto.setVerdict(JsonParseUtil.getStr(jsonObject, "verdict", "结论"));
            try {
                Object s = jsonObject.get("score");
                if (s instanceof Number n) {
                    dto.setScore(n.doubleValue());
                } else if (s != null) {
                    String ss = String.valueOf(s).trim();
                    if (StrUtil.isNotBlank(ss)) {
                        dto.setScore(Double.parseDouble(ss));
                    }
                }
            } catch (Exception ignored) {
            }
            dto.setRewrite(JsonParseUtil.getStr(jsonObject, "rewrite", "改写"));
            return dto;
        } catch (Exception e) {
            if (e instanceof BusinessException be) {
                throw be;
            }
            log.error("解析语义分析结果失败，sessionId = {}, content = {}", sessionId, rawContent, e);
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "语义分析结果解析失败，请稍后重试。");
        }
    }
}
