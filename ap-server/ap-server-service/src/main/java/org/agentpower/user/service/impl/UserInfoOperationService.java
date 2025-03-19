package org.agentpower.user.service.impl;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.crypto.digest.MD5;
import org.agentpower.api.CommonResponse;
import org.agentpower.service.secure.codec.OutputCodec;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.Recognizer;
import org.agentpower.user.model.UserInfoVo;
import org.agentpower.user.model.UserModel;
import org.agentpower.user.model.request.UserLoginRequest;
import org.agentpower.user.model.request.UserRegisterRequest;
import org.agentpower.user.repository.UserRepo;
import org.apache.tika.parser.mailcommons.MailUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

public class UserInfoOperationService {
    private final UserRepo userRepo;

    public UserInfoOperationService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public UserInfoVo getUserInfo(String id) {
        return userRepo.findById(id).map(UserInfoVo::fromUserModel).orElse(null);
    }
}
