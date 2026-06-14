package com.coze.aicoze.config;

import com.coze.aicoze.common.EncryptedResult;
import com.coze.aicoze.common.Result;
import com.coze.aicoze.util.AesUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应加密拦截器
 * 对响应数据进行加密
 */
@ControllerAdvice
@RequiredArgsConstructor
public class EncryptResponseAdvice implements ResponseBodyAdvice<Object> {

    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper;

    @Value("${encrypt.enabled:true}")
    private boolean encryptEnabled;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 处理所有响应，在beforeBodyWrite中判断是否需要加密
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        // 如果未启用加密，直接返回
        if (!encryptEnabled) {
            return body;
        }

        // 排除不需要加密的路径
        String path = request.getURI().getPath();
        if (path.contains("/swagger-ui") || 
            path.contains("/v3/api-docs") || 
            path.contains("/doc.html") ||
            path.contains("/chat/test") ||
            path.contains("/chat/stream")) {
            return body;
        }

        // 只加密Result类型的响应
        if (!(body instanceof Result)) {
            return body;
        }

        try {
            // 将响应体转换为JSON字符串
            String jsonData = objectMapper.writeValueAsString(body);
            
            // 验证JSON数据不为空
            if (jsonData == null || jsonData.isEmpty()) {
                return EncryptedResult.error("响应数据为空");
            }
            
            // 加密数据
            String encryptedData = aesUtil.encrypt(jsonData);
            
            // 验证加密后的数据不为空
            if (encryptedData == null || encryptedData.isEmpty()) {
                return EncryptedResult.error("数据加密失败：加密结果为空");
            }
            
            // 返回加密后的结果
            return EncryptedResult.success(encryptedData);
        } catch (Exception e) {
            // 加密失败，返回错误信息
            e.printStackTrace();
            return EncryptedResult.error("数据加密失败: " + e.getMessage());
        }
    }
}

