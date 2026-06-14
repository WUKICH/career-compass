package com.coze.aicoze.controller;

import com.coze.aicoze.annotation.Idempotent;
import com.coze.aicoze.common.Result;
import com.coze.aicoze.entity.User;
import com.coze.aicoze.service.UserService;
import com.coze.aicoze.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Idempotent(required = false)
    @PostMapping()
    public Result createUser(@RequestBody User user) {
        userService.createUser(user);
        
        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getName());
        
        // 返回Token和用户ID
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getName());
        
        return Result.success(data);
    }
}
