package org.agentpower.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.agentpower.api.AgentPowerFunction;

@Builder
@AllArgsConstructor
public class AgentFunctionDto implements AgentPowerFunction {
    private String functionName;
    private String functionDesc;
    private String functionParamSchema;

    @Override
    public String functionName() {
        return functionName;
    }

    @Override
    public String functionDesc() {
        return functionDesc;
    }

    @Override
    public String functionParamSchema() {
        return functionParamSchema;
    }
}