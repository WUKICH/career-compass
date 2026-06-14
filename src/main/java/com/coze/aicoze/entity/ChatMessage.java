package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer participantId;
    private String role;
    private String agentType;
    private String message;
    private Integer tokenUsage;
    private LocalDateTime createdAt;
}
