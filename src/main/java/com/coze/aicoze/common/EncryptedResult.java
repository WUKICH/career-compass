package com.coze.aicoze.common;

import lombok.Data;

/**
 * 加密响应结果
 * 用于包装加密后的数据
 */
@Data
public class EncryptedResult {
    private Integer code;
    private String msg;
    private String data;  // 加密后的数据（Base64字符串）

    public static EncryptedResult success(String encryptedData) {
        EncryptedResult r = new EncryptedResult();
        r.setCode(1);
        r.setMsg("success");
        r.setData(encryptedData);
        return r;
    }

    public static EncryptedResult error(String message) {
        return error(BizErrorCodes.UNKNOWN_SYSTEM, message);
    }

    public static EncryptedResult error(Integer code, String message) {
        EncryptedResult r = new EncryptedResult();
        r.setCode(code);
        r.setMsg(message);
        r.setData(null);
        return r;
    }
}

