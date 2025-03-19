package org.agentpower.configuration.client;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


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

    /** 客户端访问代理服务的url */
    private String serviceUrl;
    /** 客户端访问代理服务的请求头 */
    private String headers;
    /** 代理服务加密方式 */
    private String serviceAlgorithm;
    /** 代理服务公钥 信息加密后发送 */
    private String serviceKeyForEncode;
    /** 客户端的token认证类型 */
    private String recognizerType;
    /** 客户端认证头 */
    private String recognizerHeaderField;
    /** 客户端秘钥等信息 */
    private String recognizerProperties;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
