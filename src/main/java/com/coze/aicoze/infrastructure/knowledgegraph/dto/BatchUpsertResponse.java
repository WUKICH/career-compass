package com.coze.aicoze.infrastructure.knowledgegraph.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 批量写入成功响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpsertResponse {
    /**
     * 状态码，0 表示成功
     */
    private int code;

    /**
     * 消息描述
     */
    private String message;

    /**
     * 成功写入的人才数量
     */
    private int count;
}