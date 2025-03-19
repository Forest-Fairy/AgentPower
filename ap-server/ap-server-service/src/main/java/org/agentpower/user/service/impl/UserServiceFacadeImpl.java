package org.agentpower.user.service.impl;

import jakarta.annotation.Nullable;
import org.agentpower.api.CommonResponse;
import org.agentpower.service.Globals;
import org.agentpower.service.secure.codec.OutputCodec;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.Recognizer;
import org.agentpower.user.model.UserInfoVo;
import org.agentpower.user.model.request.*;
import org.agentpower.user.repository.UserRepo;
import org.agentpower.user.service.UserServiceFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class UserServiceFacadeImpl implements UserServiceFacade {

    private final UserLoginService loginService;
    private final UserInfoOperationService operationService;

    public UserServiceFacadeImpl(OutputCodec outputCodec, Recognizer recognizer, RedisTemplate<String, String> redisTemplate, UserRepo userRepo) {
        loginService = new UserLoginService(outputCodec, recognizer, redisTemplate, userRepo);
        operationService = new UserInfoOperationService(userRepo);
    }

    @Override
    public CommonResponse<String> login(UserLoginRequest loginRequest) {
        if (StringUtils.isNotBlank(loginRequest.getInfoToken())) {
            if (StringUtils.isNotBlank(loginRequest.getPassword())) {
                return loginService.loginWithPassword(loginRequest);
            } else if (StringUtils.isNotBlank(loginRequest.getCode())) {
                return loginService.loginWithCode(loginRequest);
            }
        }
        return CommonResponse.failure("登录信息有误，密码或验证码不能为空");
    }

    @Override
    public CommonResponse<String> register(UserRegisterRequest registerRequest) {
        if (StringUtils.containsIgnoreCase(registerRequest.getType(), "mail")) {
            if (StringUtils.isNoneBlank(registerRequest.getEmail(), registerRequest.getEmailCode())) {
                return loginService.registerWithEmail(registerRequest);
            }
            return CommonResponse.failure("注册信息有误，邮箱注册码不能为空");
        } else if (StringUtils.containsIgnoreCase(registerRequest.getType(), "phone")) {
            if (StringUtils.isNoneBlank(registerRequest.getPhone(), registerRequest.getPhoneCode())) {
                return loginService.registerWithPhone(registerRequest);
            }
            return CommonResponse.failure("注册信息有误，手机验证码不能为空");
        }
        return CommonResponse.failure("注册信息有误，不支持该注册类型：" + registerRequest.getType());
    }

    @Override
    public CommonResponse<String> logout() {
        return CommonResponse.success("退出成功");
    }

    @Override
    public CommonResponse<UserInfoVo> getUserInfo() {
        LoginUserVo loginUser = Globals.User.getLoginUser();
        return Optional.ofNullable(operationService.getUserInfo(loginUser.getId()))
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure("未找到用户信息"));
    }

    @Override
    public CommonResponse<String> modifyPasswordOrUsername(UserPasswordOrUsernameModifyRequest passwordOrUsernameModifyRequest) {
        return null;
    }

    @Override
    public CommonResponse<String> modifyEmailOrMobilePhone(UserEmailOrPhoneModifyRequest emailOrPhoneModifyRequest) {
        return null;
    }

    @Override
    public CommonResponse<String> modifyInfo(UserBaseInfoModifyRequest baseInfoModifyRequest, @Nullable MultipartFile avatar) {
        return null;
    }

    @Override
    public CommonResponse<String> unregister(UserUnregisterRequest unregisterRequest) {
        return null;
    }
}
