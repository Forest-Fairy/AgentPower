package org.agentpower.api;

import java.util.Map;

public interface AgentPowerFunction {
    /**
     * @return 函数名称
     */
    String functionName();

    /**
     * @return 函数描述
     */
    String functionDesc();

    /**
     * @return 函数参数描述
     */
    Map<String, Object> functionParamDesc();

}
