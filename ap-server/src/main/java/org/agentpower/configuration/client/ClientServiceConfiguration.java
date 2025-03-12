package org.agentpower.configuration.client;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.agentpower.api.Constants;
import org.agentpower.common.JwtUtil;
import org.agentpower.common.RSAUtil;
import org.agentpower.user.model.UserModel;
import org.agentpower.user.vo.LoginUserVo;

import java.util.Base64;

/**
 * 用户客户端服务配置
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ClientServiceConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String userId;

    /* 客户端访问代理服务的url */
    private String serviceUrl;
    /* 客户端访问代理服务的请求头 */
    private String headers;
    /* 代理服务公钥 用户信息加密后交给浏览器客户端 */
    private String servicePublicKey;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;

    public String generateAuthorization(String requestId, LoginUserVo user) {
        JwtUtil.createJWT(
                requestId,
                JSON.toJSONString(user),
                "client-"+id,
                Constants.JWT_EXPIRED_DURATION,
                null,
                Constants.JWT_SECRET
        )
        return RSAUtil.encrypt(Constants.CONFIG_PREFIX + userId, RSAUtil.generatePublicKey(RSAUtil.ALGORITHM, servicePublicKey));
    }
}
