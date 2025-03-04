package org.agentpower.api;

public interface AgentPowerFunctionDefinition {
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

    record FunctionDefinition(String functionName, String functionDesc, String functionParamSchema) implements AgentPowerFunctionDefinition {
        public static FunctionDefinition build(AgentPowerFunctionDefinition function) {
            return new FunctionDefinition(function.functionName(), function.functionDesc(), function.functionParamSchema());
        }
    }

}
