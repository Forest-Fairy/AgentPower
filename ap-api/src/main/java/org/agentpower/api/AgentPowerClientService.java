package org.agentpower.api;

import java.util.List;
import java.util.Map;

public interface AgentPowerClientService {
    /**
     * @return tool-call result
     */
    FunctionRequest.CallResult call(String functionName, Map<String, Object> params);

    /**
     * @return all functions in client
     */
    List<? extends AgentPowerFunctionDefinition> listFunctions();

}
