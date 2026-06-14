package com.coze.aicoze.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注册JWT拦截器和CORS配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .order(0)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 排除不需要JWT验证的路径
                        "/user",  // 用户注册/创建接口（需要生成token）
                        "/chat/init",  // 初始化聊天接口（会生成token）
                        "/full/chat/init",  // 完全版初始化（会生成 token）
                        "/chat/collect",  // 对话式收集个人信息（不落库；token 在 /chat/collect/confirm）
                        "/chat/collect/confirm",  // 确认页提交（生成 token）
                        "/chat/collect/preview-skill",  // 技能预览（NLP+收集 Agent，不落库）
                        "/chat/collect/resume/parse",  // 简历解析预览（不落库）
                        "/config/features",  // 前端特性开关（无 JWT）
                        "/chat/test",  // 测试接口
                        "/swagger-ui/**",  // Swagger UI
                        "/v3/api-docs/**",  // API文档
                        "/doc.html",  // Knife4j文档
                        "/favicon.ico",  // 图标
                        "/error"  // 错误页面
                );
        registry.addInterceptor(rateLimitInterceptor)
                .order(10)
                .addPathPatterns("/**");
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

