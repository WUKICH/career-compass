package com.coze.aicoze.workflow.guard;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 主聊阶段本地输入校验：拦截无意义短句，避免进入 Coze Chat Bot 后被「口头推进流程」。
 */
@Component
@RequiredArgsConstructor
public class MainChatInputGuard {

    private static final String DEFAULT_REPLY =
            "没太理解你的意思～可以围绕职业目标、核心技能或期望城市多说几句；准备好后也可点击「开始能力测评」。";

    private final WorkflowProperties workflowProperties;

    /**
     * @return 非空时表示应拦截并直接返回给用户的文案
     */
    public Optional<String> checkLocal(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return Optional.of("请输入有效内容后再发送。");
        }
        String trimmed = userMessage.trim();
        WorkflowProperties.MainChat cfg = workflowProperties.getMainChat();
        int minLen = cfg.getMinLength();
        if (trimmed.length() < minLen) {
            return Optional.of(replyOrDefault(cfg.getShortInputReply()));
        }
        if (isRepeatedSingleChar(trimmed)) {
            return Optional.of(replyOrDefault(cfg.getGibberishReply()));
        }
        if (!containsCjk(trimmed) && trimmed.length() < cfg.getMinLatinLength()) {
            return Optional.of(replyOrDefault(cfg.getGibberishReply()));
        }
        if (isMostlyNonWordChars(trimmed)) {
            return Optional.of(replyOrDefault(cfg.getGibberishReply()));
        }
        return Optional.empty();
    }

    private String replyOrDefault(String configured) {
        return StrUtil.isBlank(configured) ? DEFAULT_REPLY : configured;
    }

    private static boolean containsCjk(String s) {
        return s.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private static boolean isRepeatedSingleChar(String s) {
        if (s.length() < 3) {
            return false;
        }
        return s.chars().distinct().count() == 1;
    }

    private static boolean isMostlyNonWordChars(String s) {
        long wordish = s.codePoints().filter(cp ->
                Character.isLetterOrDigit(cp)
                        || Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN).count();
        return wordish == 0;
    }
}
