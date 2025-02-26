package org.agentpower.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;


public class AgentPowerToolCallbackResolver implements ToolCallbackResolver {

    public static final String callbackPrefix = "AgentPower-";
    @Override
    public FunctionCallback resolve(String toolName) {
        return toolName.startsWith(callbackPrefix) ? new AgentPowerToolCallback(toolName) : null;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        public AgentPowerToolCallbackResolver build() {
            return new AgentPowerToolCallbackResolver();
        }
    }
}
