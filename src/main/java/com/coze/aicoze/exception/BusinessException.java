package com.coze.aicoze.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
    public BusinessException(Integer code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }
}
