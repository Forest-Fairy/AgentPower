package org.agentpower.infrastructure;

import lombok.Getter;
import org.agentpower.api.Constants;
import org.agentpower.common.RSAUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties({
        AgentPowerClientConfigurations.AgentPowerServerProperties.class
})
public class AgentPowerClientConfigurations {
    @Getter
    private static byte[] jwtSecret;
    @Getter
    private static PublicKey publicKey;
    @Getter
    private static PrivateKey privateKey;

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

    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "server")
    static class AgentPowerServerProperties implements InitializingBean {
        private Integer port;
        private String publicKey;
        private String privateKey;
        private String jwtSecret;
        @Override
        public void afterPropertiesSet() throws Exception {
            if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey)) {
                KeyPair keyPair = RSAUtil.generateKeyPair(RSAUtil.ALGORITHM);
                publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
                privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            }
            AgentPowerClientConfigurations.publicKey = RSAUtil.generatePublicKey(RSAUtil.ALGORITHM, publicKey);
            AgentPowerClientConfigurations.privateKey = RSAUtil.generatePrivateKey(RSAUtil.ALGORITHM, privateKey);
            if (! StringUtils.isBlank(jwtSecret)) {
                AgentPowerClientConfigurations.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
