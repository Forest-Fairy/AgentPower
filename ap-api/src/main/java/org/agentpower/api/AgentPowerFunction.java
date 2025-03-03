package org.agentpower.api;

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
     * @return 函数参数格式 -> 参考 spring ai SpringBeanToolCallbackResolver 如何封装成字符串格式
     */
    String functionParamSchema();

    record Function(String functionName, String functionDesc, String functionParamSchema) implements AgentPowerFunction {
        public static Function build(AgentPowerFunction function) {
            return new Function(function.functionName(), function.functionDesc(), function.functionParamSchema());
        }
    }

}
