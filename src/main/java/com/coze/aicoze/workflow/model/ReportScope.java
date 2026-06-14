package com.coze.aicoze.workflow.model;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;

/**
 * 阶段性报告可见范围（与前端快捷入口 scope 查询参数一致）。
 */
public enum ReportScope {
    /** 仅个人信息 */
    PROFILE,
    /** 个人信息 + 简历优化建议 */
    RESUME,
    /** 核心技能评估 */
    SKILL,
    /** 技能 + 霍兰德兴趣 */
    CAREER,
    /** 技能 + 霍兰德 + 初步/共识岗位推荐 */
    RECOMMENDATION,
    /** 含共识岗位推荐在内的完整报告 */
    FULL;

    public static ReportScope parse(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "缺少报告范围 scope");
        }
        return switch (raw.trim().toLowerCase()) {
            case "profile" -> PROFILE;
            case "resume" -> RESUME;
            case "skill" -> SKILL;
            case "career" -> CAREER;
            case "recommendation" -> RECOMMENDATION;
            case "full" -> FULL;
            default -> throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS,
                    "无效的报告范围，可选：profile、resume、skill、career、recommendation、full");
        };
    }
}
