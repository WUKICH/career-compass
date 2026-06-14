package com.coze.aicoze.controller;

import com.coze.aicoze.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 前端可读的特性开关（无需鉴权），与 {@code application.yml} 中 {@code app.*} 对齐。
 */
@RestController
@RequestMapping("/config")
public class ClientFeaturesController {

    @Value("${app.full-version-temporarily-closed:true}")
    private boolean fullVersionTemporarilyClosed;

    @GetMapping("/features")
    public Result<Map<String, Boolean>> features() {
        return Result.success(Map.of("fullVersionTemporarilyClosed", fullVersionTemporarilyClosed));
    }
}
