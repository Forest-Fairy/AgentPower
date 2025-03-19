package org.agentpower.user.service;

import jakarta.annotation.Nullable;
import org.agentpower.api.CommonResponse;
import org.agentpower.user.model.UserInfoVo;
import org.agentpower.user.model.request.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserServiceFacade {
    /**
     * 登陆
     * @param loginRequest 登陆请求
     * @return token
     */
    CommonResponse<String> login(UserLoginRequest loginRequest);

    /**
     * 注册
     * @param registerRequest 注册请求
     * @return token
     */
    CommonResponse<String> register(UserRegisterRequest registerRequest);

    /**
     * 登出
     */
    CommonResponse<String> logout();

    /**
     * 获取用户信息
     * @return 用户信息
     */
    CommonResponse<UserInfoVo> getUserInfo();

    /**
     * 修改密码或用户名
     * @param passwordOrUsernameModifyRequest 修改密码或用户名请求
     * @return 修改结果
     */
    CommonResponse<String> modifyPasswordOrUsername(UserPasswordOrUsernameModifyRequest passwordOrUsernameModifyRequest);

    /**
     * 修改邮箱或手机号
     * @param emailOrPhoneModifyRequest 修改邮箱或手机号请求
     * @return 修改结果
     */
    CommonResponse<String> modifyEmailOrMobilePhone(UserEmailOrPhoneModifyRequest emailOrPhoneModifyRequest);

    /**
     * 修改用户信息
     * @param baseInfoModifyRequest 修改用户信息请求
     * @return 修改结果
     */
    CommonResponse<String> modifyInfo(UserBaseInfoModifyRequest baseInfoModifyRequest, @Nullable MultipartFile avatar);

    /**
     * 注销
     * @param unregisterRequest 注销请求
     * @return 注销结果
     */
    CommonResponse<String> unregister(UserUnregisterRequest unregisterRequest);
}
