package org.agentpower.infrastracture;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import org.agentpower.agent.tool.AgentPowerToolCallbackResolver;
import org.springdoc.core.service.OpenAPIService;
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
        @Bean
        ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                                  List<FunctionCallback> functionCallbacks, List<ToolCallbackProvider> tcbProviders) {
            List<FunctionCallback> allFunctionAndToolCallbacks = new ArrayList<>(functionCallbacks);
            tcbProviders.stream().map(pr -> List.of(pr.getToolCallbacks())).forEach(allFunctionAndToolCallbacks::addAll);

            var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);

            var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
                    .applicationContext(applicationContext)
                    .build();
            // TODO 验证下该Bean是否会被解析
            var agentPowerToolCallbackResolver = AgentPowerToolCallbackResolver.builder()
                    .applicationContext(applicationContext)
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
                    .version());
        }
    }

}
