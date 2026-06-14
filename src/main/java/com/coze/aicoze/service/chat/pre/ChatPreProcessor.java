package com.coze.aicoze.service.chat.pre;

import com.coze.aicoze.dto.ChatPayload;

import java.util.Optional;

/**
 * 主对话前置处理（可插拔）。按 {@link #getOrder()} 升序执行；任一环节返回非空则短路，不再调用后续处理器与主 Chat Agent。
 * <p>
 * 顺序建议：数值越小越靠前。预留区间——本地规则（违禁词等）100～500；需要模型调用的闸门 1000+。
 */
public interface ChatPreProcessor {

    int getOrder();

    /**
     * @return {@link Optional#empty()} 表示放行；否则直接作为 {@code chat} 的响应返回给前端
     */
    Optional<ChatPayload> process(ChatPreProcessContext context);
}
