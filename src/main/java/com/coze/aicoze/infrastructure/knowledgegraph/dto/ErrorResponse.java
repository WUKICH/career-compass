package com.coze.aicoze.infrastructure.knowledgegraph.dto;// -------------------- 错误响应 DTO（可选）-------------------

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 批量写入失败响应（HTTP 500 时返回）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /**
     * 错误详情
     */
    private String detail;
}