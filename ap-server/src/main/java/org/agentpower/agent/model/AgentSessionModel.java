package org.agentpower.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * 会话ID
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionModel {
    @Id
    private String id;
    private String name;
    private String userId;

    private String agentConfigId;
    private String agentClientConfigId;


    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}
