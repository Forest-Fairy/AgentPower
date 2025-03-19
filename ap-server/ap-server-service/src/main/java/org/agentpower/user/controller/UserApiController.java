package org.agentpower.user.controller;

import org.agentpower.api.CommonResponse;
import org.agentpower.service.secure.recognization.AuthRequired;
import org.agentpower.user.model.UserInfoVo;
import org.agentpower.user.model.request.UserBaseInfoModifyRequest;
import org.agentpower.user.model.request.UserLoginRequest;
import org.agentpower.user.model.request.UserRegisterRequest;
import org.agentpower.user.service.UserServiceFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/api")
public class UserApiController {

    private final UserServiceFacade userServiceFacade;

    public UserApiController(UserServiceFacade userServiceFacade) {
        this.userServiceFacade = userServiceFacade;
    }

    @PostMapping("/login")
    @AuthRequired.Type(AuthRequired.Types.NONE)
    public CommonResponse<String> login(UserLoginRequest loginRequest) {
        return userServiceFacade.login(loginRequest);
    }

    @PostMapping("/register")
    @AuthRequired.Type(AuthRequired.Types.NONE)
    public CommonResponse<String> register(UserRegisterRequest registerRequest) {
        return userServiceFacade.register(registerRequest);
    }

    @GetMapping("/logout")
    public CommonResponse<String> logout() {
        return userServiceFacade.logout();
    }

    @GetMapping("/getUserInfo")
    public CommonResponse<UserInfoVo> getUserInfo() {
        return userServiceFacade.getUserInfo();
    }

    @PostMapping("/modifyInfo")
    public CommonResponse<String> modifyInfo(
            UserBaseInfoModifyRequest infoModifyRequest, MultipartFile avatar) {
        return userServiceFacade.modifyInfo(infoModifyRequest, avatar);
    }

}
