package com.coze.aicoze.config;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.common.Result;
import com.coze.aicoze.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT拦截器
 * 用于验证请求中的JWT Token
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS请求直接放行（跨域预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头中获取token
        String token = request.getHeader("token");
        
        // 如果token为空，尝试从Authorization头获取
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 如果没有token，返回401
        if (token == null || token.isEmpty()) {
            return handleUnauthorized(response, "缺少认证Token");
        }

        // 验证token
        if (!jwtUtil.validateToken(token)) {
            return handleUnauthorized(response, "Token无效或已过期");
        }

        // 将用户ID存储到request中，供后续使用
        Integer userId = jwtUtil.getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }

        return true;
    }

    /**
     * 处理未授权的情况
     */
    private boolean handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(BizErrorCodes.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }
}

