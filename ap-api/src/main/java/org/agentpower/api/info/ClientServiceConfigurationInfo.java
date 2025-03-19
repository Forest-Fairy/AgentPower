package org.agentpower.api.info;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class ClientServiceConfigurationInfo {
    /** 代理服务加密方式 */
    private String algorithm;
    /** 代理服务公钥 用户信息加密后交给浏览器客户端 */
    private String keyForEncode;
    /** 客户端的token认证类型 */
    private String recognizerType;
    /** 客户端认证头 */
    private String recognizerHeader;
//    配置信息为加密信息 需要手动获取
//    或先通过发请求给服务端 服务端校验后发送加密消息给浏览器
//    浏览器利用加密信息去访问客户端接口 客户端再校验消息的安全性
//    /** 客户端秘钥等信息 */
//    private String recognizerProperties;
}
