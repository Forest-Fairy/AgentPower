package org.agentpower.configuration.model;

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
public class UserClientServiceConfiguration {
    @Id
    private String id;
    private String userId;
    private String name;

    /* 代理服务url */
    private String serviceUrl;
    /* 代理服务公钥 */
    private String servicePKey;
    /* 代理服务私钥 */
    private String serviceSKey;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
