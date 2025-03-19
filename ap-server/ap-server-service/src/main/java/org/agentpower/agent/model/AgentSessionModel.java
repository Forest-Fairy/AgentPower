package org.agentpower.agent.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 会话ID
 */
@Entity
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String userId;

    /** 选择的代理配置 */
    private String agentModelConfigId;
    /** 选择的客户端配置 */
    private String agentClientServiceConfigId;


    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
