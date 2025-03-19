package org.agentpower.user.model;


import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class UserInfoVo {
    private String id;
    private String uid;
    private String username;
    private String email;
    private String phone;

    private String nickname;
    private String avatarUrl;
    private String sign;

    private String updatedTime;
    public static UserInfoVo fromUserModel(UserModel userModel) {
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userModel, userInfoVo);
        return userInfoVo;
    }
}
