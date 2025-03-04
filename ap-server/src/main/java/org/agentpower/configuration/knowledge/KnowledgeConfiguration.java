package org.agentpower.configuration.knowledge;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class KnowledgeConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
}