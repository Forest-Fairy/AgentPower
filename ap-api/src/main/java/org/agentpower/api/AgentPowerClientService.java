package org.agentpower.api;

import java.util.List;

public interface AgentPowerClientService {
    /**
     * @param functionRequest request
     * @return tool-call result
     */
    String call(FunctionRequest functionRequest);

    /**
     * @return all functions in client
     */
    List<AgentPowerFunction> listFunctions();

}
