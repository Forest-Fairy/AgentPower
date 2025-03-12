package org.agentpower.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.Getter;
import org.agentpower.agent.service.AgentChatService;
import org.agentpower.agent.tool.AgentPowerToolCallbackResolver;
import org.agentpower.api.Constants;
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.ConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties({
        AgentPowerServerConfigurations.AgentPowerServerProperties.class
})
public class AgentPowerServerConfigurations {
    @Getter
    private static byte[] jwtSecret;
    @Getter
    private static PublicKey publicKey;
    @Getter
    private static PrivateKey privateKey;

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

    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "server")
    static class AgentPowerServerProperties implements InitializingBean {
        private Integer port;
        private String publicKey;
        private String privateKey;
        private String jwtSecret;
        private Long expireDuration;
        @Override
        public void afterPropertiesSet() throws Exception {
            if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey)) {
                KeyPair keyPair = RSAUtil.generateKeyPair(RSAUtil.ALGORITHM);
                publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
                privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            }
            AgentPowerServerConfigurations.publicKey = RSAUtil.generatePublicKey(RSAUtil.ALGORITHM, publicKey);
            AgentPowerServerConfigurations.privateKey = RSAUtil.generatePrivateKey(RSAUtil.ALGORITHM, privateKey);
            if (! StringUtils.isBlank(jwtSecret)) {
                AgentPowerServerConfigurations.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
