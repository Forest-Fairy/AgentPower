package org.agentpower.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "agent-power")
public class AgentPowerAutoConfiguration {

//    @Bean
//    ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
//                                              List<FunctionCallback> functionCallbacks, List<ToolCallbackProvider> tcbProviders) {
//        List<FunctionCallback> allFunctionAndToolCallbacks = new ArrayList<>(functionCallbacks);
//        tcbProviders.stream().map(pr -> List.of(pr.getToolCallbacks())).forEach(allFunctionAndToolCallbacks::addAll);
//
//        var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);
//
//        var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
//                .applicationContext(applicationContext)
//                .build();
//        return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver));
//    }

}
