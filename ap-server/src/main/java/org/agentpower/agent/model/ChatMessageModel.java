package org.agentpower.agent.model;


import lombok.*;
import org.springframework.data.annotation.Id;

/**
 * 会话ID
 */
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageModel {
    @Id
    private String id;
    private String userId;

    // content
    /* 会话id */
    private String sessionId;
    /* 问题内容 */
    private String question;
    /* 系统预先知识 */
    private String systemKnowledge;

    // settings
    /* 是否启用向量库 */
    private boolean enableVectorStore;
    /* 客户端增强id */
    private String clientAgentServiceId;
    /* 资源提供者列表 */
    private String resourceProviders;



    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}