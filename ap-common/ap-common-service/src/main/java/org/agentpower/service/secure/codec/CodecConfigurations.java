package org.agentpower.service.secure.codec;

import lombok.Getter;
import org.agentpower.api.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties({
        CodecConfigurations.AgentPowerCodecProperties.class
})
public class CodecConfigurations {
    private AgentPowerCodecProperties properties;
    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "codec")
    static class AgentPowerCodecProperties implements InitializingBean {
        private boolean enabled;
        private String type;
        private String keyForEncode;
        private String keyForDecode;
        @Override
        public void afterPropertiesSet() throws Exception {
            if (enabled && StringUtils.isNoneBlank(keyForDecode)) {
                CodecConfigurations.decoder = CodecProvider.GenerateDecoder(type, keyForDecode);
            }
        }
    }

    @Bean
    public InputDecoder decoder() {
        Decoder decoder;
        if (properties.enabled && StringUtils.isNotBlank(properties.keyForDecode)) {
            decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
        } else {
            decoder = null;
        }
        return new InputDecoder(decoder);
    }

    @Bean
    public OutputEncoder encoder() {
        Decoder decoder;
        if (properties.enabled && StringUtils.isNotBlank(properties.keyForDecode)) {
            decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
        } else {
            decoder = null;
        }
        return new OutputEncoder(decoder);
    }
}
