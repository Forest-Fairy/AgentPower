package org.agentpower.service.secure.codec;

import jakarta.annotation.Resource;
import org.agentpower.api.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties({
        AgentPowerCodecConfiguration.AgentPowerCodecProperties.class
})
public class AgentPowerCodecConfiguration {
    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "codec")
    static class AgentPowerCodecProperties {
        private boolean enabled;
        private String type;
        private String keyForEncode;
        private String keyForDecode;
    }

    @Resource
    private AgentPowerCodecProperties properties;

    @Bean
    InputCodec decoder() {
        Decoder decoder;
        if (properties.enabled && StringUtils.isNotBlank(properties.keyForDecode)) {
            decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
        } else {
            decoder = null;
        }
        return new InputCodec(decoder);
    }

    @Bean
    OutputCodec encoder() {
        Decoder decoder;
        if (properties.enabled && StringUtils.isNotBlank(properties.keyForDecode)) {
            decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
        } else {
            decoder = null;
        }
        return new OutputCodec(decoder);
    }

    /**
     * @return 编码类型
     */
    public String codecAlgorithm() {
        return properties.type;
    }

    /**
     * @return 编码秘钥
     */
    public String keyForEncode() {
        return properties.keyForEncode;
    }

}
