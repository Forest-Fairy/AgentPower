package org.agentpower.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户信息修改请求
 */
@Getter
@AllArgsConstructor
public class UserBaseInfoModifyRequest {
    private String nickname;
    private String sign;
}
