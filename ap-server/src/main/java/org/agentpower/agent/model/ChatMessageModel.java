package org.agentpower.agent.model;


import com.alibaba.fastjson2.JSON;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.agentpower.agent.dto.ChatMediaResourceProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 会话ID
 */
@Data
@Entity
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;

    // content
    /** 会话id */
    private String sessionId;
    /** 请求id */
    private String requestId;
    /** 系统预先知识 */
    private String systemKnowledge;

    /** 问题内容 */
    private String textContent;

    // settings
    /** 是否启用向量库 */
    private boolean enableVectorStore;
    /** 客户端代理模型配置id */
    private String agentModelConfigurationId;
    /** 客户端代理增强服务配置id */
    private String clientAgentServiceConfigurationId;
    /** 资源提供者列表 */
    private String resourceProviders;


    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}