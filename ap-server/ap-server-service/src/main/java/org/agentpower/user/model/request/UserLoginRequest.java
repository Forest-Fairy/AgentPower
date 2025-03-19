package org.agentpower.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户登录请求
 */
@Getter
@AllArgsConstructor
public class UserLoginRequest {
    private String infoToken;
    private String password;
    private String code;
}
