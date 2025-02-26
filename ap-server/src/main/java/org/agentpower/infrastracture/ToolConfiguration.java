package org.agentpower.infrastracture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ToolConfiguration {

    @Bean
    ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                              List<FunctionCallback> functionCallbacks, List<ToolCallbackProvider> tcbProviders) {

        List<FunctionCallback> allFunctionAndToolCallbacks = new ArrayList<>(functionCallbacks);
        tcbProviders.stream().map(pr -> List.of(pr.getToolCallbacks())).forEach(allFunctionAndToolCallbacks::addAll);

        var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);

        var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
                .applicationContext(applicationContext)
                .build();

        return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver));
    }
}
