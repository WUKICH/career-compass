package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_event")
public class FlowEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer participantId;
    private String fromPhase;
    private String toPhase;
    private String triggerName;
    private String detail;
    private LocalDateTime createdAt;
}
