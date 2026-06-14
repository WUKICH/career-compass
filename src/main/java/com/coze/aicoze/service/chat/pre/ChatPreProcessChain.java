package com.coze.aicoze.service.chat.pre;

import com.coze.aicoze.dto.ChatPayload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ChatPreProcessChain {

    private final List<ChatPreProcessor> processors;

    public ChatPreProcessChain(List<ChatPreProcessor> processors) {
        // 通过 order 形成稳定的“前置流水线”，便于按成本/风险把轻量规则放前面。
        this.processors = processors.stream()
                .sorted(Comparator.comparingInt(ChatPreProcessor::getOrder))
                .toList();
    }

    public Optional<ChatPayload> run(ChatPreProcessContext context) {
        for (ChatPreProcessor processor : processors) {
            Optional<ChatPayload> out = processor.process(context);
            if (out.isPresent()) {
                // 任何一个前置处理器都可“短路返回”，用于拦截/澄清等场景。
                return out;
            }
        }
        return Optional.empty();
    }
}
