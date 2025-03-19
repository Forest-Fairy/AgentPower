package org.agentpower.configuration.client;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ClientServiceConfigurationService {
    private final ClientServiceConfigurationRepo repo;

    public AgentPowerFunctionDefinition getFunction(ClientServiceConfiguration configuration, String functionName) {
        // 发送消息给当前响应体 获取接口数据
        return null;
    }

}
