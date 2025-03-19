package org.agentpower.user.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户注册请求
 */
@Getter
@AllArgsConstructor
public class UserRegisterRequest {
    private String email;
    private String emailCode;

    private String phone;
    private String phoneCode;

    @NotBlank
    private String password;

    @NotBlank
    private String type;
}
