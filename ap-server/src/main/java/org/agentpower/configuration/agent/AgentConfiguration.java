package org.agentpower.configuration.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * 用户代理配置
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfiguration {
    @Id
    private String id;
    private String name;
    private String userId;

    /* 代理 平台: ollama, llm studio, etc. */
    private String agentPlatform;
    /* 代理 url */
    private String agentBaseUrl;
    /* 代理 认证头 */
    private String agentAuthorization;
    /* 代理 自定义请求头，JSON格式 */
    private String agentCustomHeaders;
    /* 代理 模型 */
    private String agentModel;
    /* 代理 微调参数，JSON格式 */
    private String agentCustomOptions;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
