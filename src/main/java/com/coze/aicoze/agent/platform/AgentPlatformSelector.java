package com.coze.aicoze.agent.platform;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentPlatformSelector {

    private final List<AgentPlatformAdapter> adapters;

    public AgentPlatformAdapter resolve(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        Map<String, AgentPlatformAdapter> adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        adapter -> adapter.provider().toLowerCase(Locale.ROOT),
                        adapter -> adapter,
                        (left, right) -> left));
        AgentPlatformAdapter adapter = adapterMap.get(normalized);
        if (adapter == null) {
            throw new IllegalStateException("No AgentPlatformAdapter configured for provider: " + provider);
        }
        return adapter;
    }
}
