package org.agentpower.configuration.model;

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
public class UserAgentConfiguration {
    @Id
    private String id;
    private String userId;
    private String name;

    /* 代理 类型: ollama, llm studio, etc. */
    private String agentType;
    /* 代理 url */
    private String agentBaseUrl;
    /* 代理 认证头 */
    private String agentAuthorization;
    /* 代理 模型 */
    private String agentModel;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
