package org.agentpower.agent.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import org.springframework.ai.chat.messages.MessageType;

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
    /** 消息类型 {@link MessageType} */
    private String messageType;

    // content
    /** 会话id */
    private String sessionId;
    /** 请求id */
    private String requestId;
    /** 系统预先知识 */
    private String systemKnowledge;
//    /** 引用记忆组数 */ 暂时不用
//    private int chatMemoryCouplesCount;

    /** 问题内容 */
    private String textContent;

    // settings
    /** 客户端代理模型配置id */
    private String agentModelConfigurationId;
    /** 客户端代理增强服务配置id */
    private String clientAgentServiceConfigurationId;
    /** 知识库id */
    private String knowledgeBaseId;
    /** 资源提供者列表 */
    private String resourceProviders;


    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}