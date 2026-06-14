package com.coze.aicoze.enums;

/**
 * 测评产物类型，合并原 skill_test / career_test / chat_end_result / feedback。
 */
public enum ArtifactType {
    SKILL_QUESTION,
    SKILL_RESULT,
    CAREER_QUESTION,
    CAREER_RESULT,
    REPORT,
    FEEDBACK;

    /** 同一会话仅保留最新一条（幂等 upsert）。 */
    public boolean isSingleton() {
        return this == SKILL_RESULT || this == CAREER_RESULT || this == REPORT || this == FEEDBACK;
    }

    /** 兼容旧 message_type 字符串。 */
    public static ArtifactType fromLegacyMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("messageType is blank");
        }
        return switch (messageType) {
            case "SKILL_QUESTION" -> SKILL_QUESTION;
            case "SKILL_RESULT" -> SKILL_RESULT;
            case "QUESTION" -> CAREER_QUESTION;
            case "MBIT_RESULT" -> CAREER_RESULT;
            case "REPORT_COMPOSED" -> REPORT;
            default -> ArtifactType.valueOf(messageType);
        };
    }
}
