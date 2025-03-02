package org.agentpower.agent.tool;

import lombok.AllArgsConstructor;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Optional;

@AllArgsConstructor
public class AgentPowerToolCallbackResolver implements ToolCallbackResolver {

    public static final String CALLBACK_PREFIX = "AgentPower-";
    private final GenericApplicationContext applicationContext;

    @Override
    public FunctionCallback resolve(String toolName) {
        if (!toolName.startsWith(CALLBACK_PREFIX)) {
            return null;
        }
        // AgentPower-clientServiceId-functionName
        String[] toolInfos = toolName.split("-", 3);
        String clientServiceId = toolInfos[1];
        String functionName = toolInfos[2];
        return Globals.Client.getToolCallback(clientServiceId, functionName);
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private GenericApplicationContext applicationContext;
        public AgentPowerToolCallbackResolver build() {
            return new AgentPowerToolCallbackResolver(applicationContext);
        }

        public Builder applicationContext(GenericApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            return this;
        }
    }
}
