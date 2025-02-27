package org.agentpower.configuration.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * 用户客户端服务配置
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceConfiguration {
    @Id
    private String id;
    private String name;
    private String userId;

    /* 代理服务url */
    private String serviceUrl;
    /* 客户端访问代理服务的请求头 */
    private String headers;
    /* 代理服务公钥 加密后交给浏览器客户端 */
    private String servicePublicKey;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
