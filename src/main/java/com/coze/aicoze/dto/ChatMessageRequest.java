package com.coze.aicoze.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String sessionId;
    private String message;
}



