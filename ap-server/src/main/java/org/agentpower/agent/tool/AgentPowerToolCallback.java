package org.agentpower.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class AgentPowerToolCallback implements ToolCallback {

    private final String toolName;
    private final ToolDefinition toolDefinition;

    public AgentPowerToolCallback(String toolName) {
        this.toolName = toolName;
        // get tool definition with thread variant
        this.toolDefinition = null;
    }

    @Override
    public @Nonnull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        // call function with client service
        JSONObject callingParams = JSON.parseObject(toolInput);
        return "";
    }
    public static int receiveRequestResult(FunctionRequest request) {
        // TODO handle result, normally is string content
        return StatusCode.REQUEST_ABORT;
    }
}
