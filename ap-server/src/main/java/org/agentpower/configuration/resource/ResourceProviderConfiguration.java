package org.agentpower.configuration.resource;


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
public class ResourceProviderConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    /* 提供者类型 */
    private String type;
    /* 提供者参数 如 oss 的服务器路径 */
    private String params;
}