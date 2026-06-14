package com.coze.aicoze.exception;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage(), e);
        Result<Object> result = new Result<>();
        result.setCode(e.getCode());
        result.setMsg(e.getMessage());
        return result;
    }

    /**
     * 上传文件超过 spring.servlet.multipart 限制（简历等）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return Result.error(
                BizErrorCodes.INVALID_SESSION_OR_PARAMS,
                "简历文件不能超过 5MB，请压缩或更换文件后重试");
    }

    /**
     * 系统未知异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(BizErrorCodes.UNKNOWN_SYSTEM, "服务暂时异常，请稍后重试");
    }
}
