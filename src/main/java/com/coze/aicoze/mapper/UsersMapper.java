package com.coze.aicoze.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coze.aicoze.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsersMapper extends BaseMapper<User> {
}
