package com.coze.aicoze.enums;

public enum AgentType {
    CHAT,         // 普通聊天Agent
    SKILL_TEST,   // 技能测试Agent
    SKILL_END,    // 技能测试结束
    CAREER_TEST,  // 职业兴趣测试Agent
    CAREER_END,   // 霍兰德测试结束
    RECOMMENDATION, // 初步岗位推荐
    CONSENSUS,    // 岗位共识多轮对话
    NLP_START,    // 自然语言处理分析开始
    NLP_END,       // 自然语言处理分析结束
    ASSOCIATION_START,  //问题联想开始
    ASSOCIATION_END,     //问题联想结束
    COLLECTION_START,   //收集信息开始
    COLLECTION_END,     //收集信息结束
    RESUME_PARSE,       //简历解析
    RESUME_OPTIMIZE,    // 简历与测评对照优化建议（CHAT_END 二次调用）
}