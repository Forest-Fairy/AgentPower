package org.agentpower.configuration.provider;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ResourceProviderConfiguration {
    @Id
    private String id;
    /* 提供者类型 */
    private String type;
    /* 提供者参数 如 oss 的服务器路径 */
    private String params;
}