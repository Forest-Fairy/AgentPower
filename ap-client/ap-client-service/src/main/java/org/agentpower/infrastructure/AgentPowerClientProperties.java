package org.agentpower.infrastructure;

import org.agentpower.api.Constants;
import org.agentpower.common.RSAUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.KeyPair;

@ConfigurationProperties(Constants.CONFIG_PREFIX)
public class AgentPowerClientProperties {
    private static AgentPowerClientProperties $SELF;
    private final Integer port;
    private final String publicKey;
    private final String privateKey;


    public AgentPowerClientProperties(
            @Value("${agent-power.server.port}") Integer port,
            @Value("${agent-power.server.public-key}") String publicKey,
            @Value("${agent-power.server.private-key}") String privateKey) {
        $SELF = this;
        this.port = port;
        if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey)) {
            KeyPair keyPair = RSAUtil.generateKeyPair(RSAUtil.ALGORITHM);
            this.publicKey = keyPair.getPublic().toString();
            this.privateKey = keyPair.getPrivate().toString();
        } else {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }
    
    public static Integer getPort() {
        return $SELF.port;
    }
    public static String getPublicKey() {
        return $SELF.publicKey;
    }
    public static String getPrivateKey() {
        return $SELF.privateKey;
    }
}