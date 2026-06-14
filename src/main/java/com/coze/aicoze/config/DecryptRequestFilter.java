package com.coze.aicoze.config;

import com.coze.aicoze.util.AesUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 请求解密过滤器
 * 对请求体进行解密
 */
public class DecryptRequestFilter implements Filter {

    private final AesUtil aesUtil;
    private final boolean encryptEnabled;

    public DecryptRequestFilter(AesUtil aesUtil, boolean encryptEnabled) {
        this.aesUtil = aesUtil;
        this.encryptEnabled = encryptEnabled;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (!encryptEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 排除不需要解密的路径
        if (isDecryptExcludedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 只处理POST/PUT/PATCH请求
        String method = httpRequest.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 文件上传（multipart）保持原始流与 Content-Type，避免被包装成 JSON
        if (isMultipartRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 读取请求体
        byte[] bodyBytes = StreamUtils.copyToByteArray(httpRequest.getInputStream());
        if (bodyBytes.length == 0) {
            chain.doFilter(request, response);
            return;
        }

        String encryptedData = new String(bodyBytes, StandardCharsets.UTF_8);

        // 尝试解密（失败则按原样传递）
        String tempDecryptedData = encryptedData;
        try {
            String trimmed = encryptedData.trim();
            // 检查是否是Base64格式（简单检查）
            if (!trimmed.isEmpty() && trimmed.matches("^[A-Za-z0-9+/=]+$")) {
                String maybeJson = aesUtil.decrypt(trimmed);

                // 验证解密后的数据是否为有效的JSON（简单检查）
                String trimmedDecrypted = maybeJson.trim();
                if (trimmedDecrypted.startsWith("{") || trimmedDecrypted.startsWith("[")) {
                    tempDecryptedData = maybeJson;
                }
            }
        } catch (Exception ignored) {
            // 保持 tempDecryptedData 为 encryptedData
        }
        final String decryptedData = tempDecryptedData;
        final byte[] decryptedBytes = decryptedData.getBytes(StandardCharsets.UTF_8);

        // 创建新的请求包装类：替换请求体并强制 Content-Type 为 JSON
        HttpServletRequest wrappedRequest = new jakarta.servlet.http.HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getContentType() {
                return "application/json;charset=UTF-8";
            }

            @Override
            public String getHeader(String name) {
                if ("content-type".equalsIgnoreCase(name)) {
                    return "application/json;charset=UTF-8";
                }
                return super.getHeader(name);
            }

            @Override
            public java.util.Enumeration<String> getHeaders(String name) {
                if ("content-type".equalsIgnoreCase(name)) {
                    return java.util.Collections.enumeration(
                            java.util.Collections.singletonList("application/json;charset=UTF-8"));
                }
                return super.getHeaders(name);
            }

            @Override
            public int getContentLength() {
                return decryptedBytes.length;
            }

            @Override
            public long getContentLengthLong() {
                return decryptedBytes.length;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decryptedBytes), StandardCharsets.UTF_8));
            }

            @Override
            public jakarta.servlet.ServletInputStream getInputStream() {
                return new jakarta.servlet.ServletInputStream() {
                    private final ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);

                    @Override
                    public int read() {
                        return bais.read();
                    }

                    @Override
                    public boolean isFinished() {
                        return bais.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(jakarta.servlet.ReadListener listener) {
                        // no-op
                    }
                };
            }
        };

        chain.doFilter(wrappedRequest, response);
    }

    private static boolean isDecryptExcludedPath(String path) {
        return path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/doc.html")
                || path.contains("/chat/test")
                || path.contains("/chat/stream")
                || path.contains("/chat/collect/resume/parse");
    }

    private static boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }
}

