package com.coze.aicoze.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ChatInitRequest {
    private String sessionId; // 可选，编辑模式下传入已有的 sessionId
    private String name;
    /** 体验版本（体验版/完全版），仅用于首轮对话上下文，可不落库 */
    private String experienceVersion;
    /** 工作年限（传输侧多为中文如 1-3年；落库为 0、1-3、3-5、5+、其他，见 {@link com.coze.aicoze.util.WorkYearsCodec}） */
    private String workYears;
    private String targetJob;
    private String skill;
    private String expectedCity; // 期望城市
    private String expectedSalary; // 期望薪资
    private String resume;
    /** 采集侧：是否经简历解析进入确认（不落 OSS 链接时仍可为 true） */
    private Boolean resumeSubmitted;
    /** 与 {@link #resumeSubmitted} 配套，简历原文摘录（截断），供 CHAT_END 对照测评 */
    private String resumeClaims;

    @Override
    public String toString() {
        return "ChatInitRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", name='" + name + '\'' +
                ", experienceVersion='" + experienceVersion + '\'' +
                ", workYears='" + workYears + '\'' +
                ", targetJob='" + targetJob + '\'' +
                ", skill='" + skill + '\'' +
                ", expectedCity='" + expectedCity + '\'' +
                ", expectedSalary='" + expectedSalary + '\'' +
                ", resume='" + resume + '\'' +
                ", resumeSubmitted=" + resumeSubmitted +
                ", resumeClaims.len=" + (resumeClaims == null ? 0 : resumeClaims.length()) +
                '}';
    }

}



