package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;

/**
 * 根据 Bot 字段与题干推断技能题是否多选，供前端渲染复选框。
 */
public final class SkillQuestionMultiSelectUtil {

    private SkillQuestionMultiSelectUtil() {
    }

    public static boolean isMultiSelect(JSONObject itemObj, String type, String stem) {
        if (itemObj != null) {
            if (Boolean.TRUE.equals(itemObj.getBool("是否多选"))
                    || Boolean.TRUE.equals(itemObj.getBool("multiSelect"))) {
                return true;
            }
            String mode = itemObj.getStr("选择模式");
            if (StrUtil.isNotBlank(mode) && mode.contains("多选")) {
                return true;
            }
        }
        if (StrUtil.isNotBlank(type) && type.contains("多选")) {
            return true;
        }
        if (StrUtil.isNotBlank(stem)) {
            if (stem.contains("(多选)") || stem.contains("（多选）")) {
                return true;
            }
        }
        return false;
    }

    /** 职业题：题干或类型含多选时同样支持 */
    public static boolean isMultiSelectCareer(JSONObject itemObj, String type, String stem) {
        return isMultiSelect(itemObj, type, stem);
    }
}
