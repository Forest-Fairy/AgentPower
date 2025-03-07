package org.agentpower.configuration.client;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.agentpower.api.Constants;
import org.agentpower.common.RSAUtil;

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
    /* 代理服务公钥 用户id加密后交给浏览器客户端 */
    private String servicePublicKey;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;

    public String generateAuthorization(String userId) {
        return RSAUtil.encrypt(RSAUtil.ALGORITHM, Constants.CONFIG_PREFIX + userId, servicePublicKey);
    }
}
