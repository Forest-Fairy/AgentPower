package org.agentpower.api;

import java.util.Map;

/**
 * 客户端的开放接口，供服务端调用
 */
public interface AgentPowerClient {

    Map<String, Object> getFileContent(String fileAbsPath);

}
