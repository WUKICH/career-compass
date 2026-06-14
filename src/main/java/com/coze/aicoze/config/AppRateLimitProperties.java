package com.coze.aicoze.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class AppRateLimitProperties {

    private boolean enabled = false;
    private String keyPrefix = "rl:v1:";
    private int requestsPerMinute = 120;
    private List<String> pathPatterns = new ArrayList<>();
}
