package com.coze.aicoze.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coze.aicoze.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
