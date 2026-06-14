package com.coze.aicoze.config;

import com.coze.aicoze.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final AesUtil aesUtil;

    @Value("${encrypt.enabled:true}")
    private boolean encryptEnabled;

    @Bean
    public FilterRegistrationBean<DecryptRequestFilter> decryptRequestFilterRegistration() {
        DecryptRequestFilter filter = new DecryptRequestFilter(aesUtil, encryptEnabled);
        FilterRegistrationBean<DecryptRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setName("decryptRequestFilter");
        registration.setOrder(1);
        return registration;
    }
}

