package org.agentpower.infrastracture;

import org.agentpower.agent.tool.AgentPowerToolCallbackResolver;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ToolConfigurations {

    @Bean
    ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                              List<FunctionCallback> functionCallbacks, List<ToolCallbackProvider> tcbProviders) {
        List<FunctionCallback> allFunctionAndToolCallbacks = new ArrayList<>(functionCallbacks);
        tcbProviders.stream().map(pr -> List.of(pr.getToolCallbacks())).forEach(allFunctionAndToolCallbacks::addAll);

        var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);

        var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
                .applicationContext(applicationContext)
                .build();
        var agentPowerToolCallbackResolver = AgentPowerToolCallbackResolver.builder()
                .applicationContext(applicationContext)
                .build();
        return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver, agentPowerToolCallbackResolver));
    }
}
