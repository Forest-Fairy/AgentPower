package org.agentpower.agent.dto;

import java.util.List;

/**
 *
 * @param enableVectorStore     启用向量数据库
 * @param clientAgentModelId    客户端代理模型配置id -> 不可为空
 * @param clientAgentServiceId  客户端代理增强配置id -> 空表示无启用
 * @param resourceProviders     资源提供者列表
 */
public record ChatMessageSetting(
        boolean enableVectorStore,
        String clientAgentModelId,
        String clientAgentServiceId,
        List<ChatMediaResourceProvider> resourceProviders) {

}