package org.agentpower.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.agentpower.agent.service.AgentChatService;
import org.agentpower.agent.tool.AgentPowerToolCallbackResolver;
import org.agentpower.configuration.ConfigurationService;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties({
        AgentPowerProperties.class
})
public class AgentPowerConfigurations {

    public static class ToolCallbackConfiguration {
        // TODO 需要验证这种方式bean是否能够被解析
        @Bean
        ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                                  List<FunctionCallback> functionCallbacks, List<ToolCallbackProvider> tcbProviders,
                                                  AgentChatService chatService, ConfigurationService configurationService) {
            List<FunctionCallback> allFunctionAndToolCallbacks = new ArrayList<>(functionCallbacks);
            tcbProviders.stream().map(pr -> List.of(pr.getToolCallbacks())).forEach(allFunctionAndToolCallbacks::addAll);

            var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);

            var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
                    .applicationContext(applicationContext)
                    .build();
            var agentPowerToolCallbackResolver = AgentPowerToolCallbackResolver.builder()
                    .applicationContext(applicationContext)
                    .chatService(chatService)
                    .configurationService(configurationService)
                    .build();
            return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver, agentPowerToolCallbackResolver));
        }
    }

    public static class SwaggerConfiguration {
        // TODO learn something about springdoc before using
        @Bean
        OpenAPI openAPI() {
            return new OpenAPI().info(new Info()
                    .title("AgentPower API Test Page")
                    .description("记得修改，统一版本信息")
                    .version(""));
        }
    }

}
