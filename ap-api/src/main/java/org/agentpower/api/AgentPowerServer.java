package org.agentpower.api;

/**
 * 服务端的开放接口，供客户端调用
 */
public interface AgentPowerServer {

    int sendingFileContent(String fileAbsPath, boolean existOrNot, byte[] fileContentIfExist);

}
