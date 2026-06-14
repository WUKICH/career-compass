package com.coze.aicoze.config;

import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@Slf4j
public class CozeConfig {
    @Value("${coze.api-token}")
    private String apiToken;
    @Value("${coze.api-url}")
    private String baseUrl;

    @Bean
    @Primary
    public CozeAPI cozeAPI() {
        log.info("初始化CozeAPI客户端，BaseURL: {}", baseUrl);
        TokenAuth authCli = new TokenAuth(apiToken);
        return new CozeAPI.Builder()
                .baseURL(baseUrl)
                .auth(authCli)
                .readTimeout(10000)
                .connectTimeout(5000)
                .build();
    }


}