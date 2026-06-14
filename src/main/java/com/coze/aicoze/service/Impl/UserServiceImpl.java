package com.coze.aicoze.service.Impl;

import com.coze.aicoze.entity.User;
import com.coze.aicoze.mapper.UsersMapper;
import com.coze.aicoze.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UsersMapper usersMapper;

    @Override
    public void createUser(User user) {
        usersMapper.insert(user);
    }
}
