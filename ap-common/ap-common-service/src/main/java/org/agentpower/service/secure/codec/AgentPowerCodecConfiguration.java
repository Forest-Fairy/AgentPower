package org.agentpower.service.secure.codec;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import org.agentpower.api.Constants;
import org.agentpower.common.RSAUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.security.KeyPair;
import java.util.Base64;


@Configuration
@EnableConfigurationProperties({
        AgentPowerCodecConfiguration.AgentPowerCodecProperties.class
})
public class AgentPowerCodecConfiguration {
    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "codec")
    static class AgentPowerCodecProperties implements InitializingBean {
        private boolean enabled;
        private String type;
        private String keyForEncode;
        private String keyForDecode;

        @Override
        public void afterPropertiesSet() throws Exception {
            if (StringUtils.isNotBlank(type)) {
                if ("auto".equalsIgnoreCase(keyForDecode)) {
                    File encode = new File("./codec/encode.key");
                    File decode = new File("./codec/decode.key");
                    byte[] keyForEncodeBytes;
                    byte[] keyForDecodeBytes;
                    if (encode.exists() && encode.isFile()
                            && decode.exists() && decode.isFile()) {
                        // 密钥对存在
                        keyForEncodeBytes = FileUtil.readBytes(encode);
                        keyForDecodeBytes = FileUtil.readBytes(decode);
                    } else {
                        encode.delete();
                        decode.delete();
                        KeyPair keyPair = RSAUtil.generateKeyPair(type);
                        keyForEncodeBytes = keyPair.getPublic().getEncoded();
                        keyForDecodeBytes = keyPair.getPrivate().getEncoded();
                        FileUtil.writeBytes(keyForEncodeBytes, encode);
                        FileUtil.writeBytes(keyForDecodeBytes, decode);
                    }
                    if (keyForEncodeBytes.length > 0 && keyForDecodeBytes.length > 0) {
                        this.keyForEncode = Base64.getEncoder().encodeToString(keyForEncodeBytes);
                        this.keyForDecode = Base64.getEncoder().encodeToString(keyForDecodeBytes);
                    }
                }
            }
        }
    }

    @Resource
    private AgentPowerCodecProperties properties;

    @Bean
    InputCodec inputCodec() {
        Decoder decoder;
        if (properties.enabled && StringUtils.isNotBlank(properties.keyForDecode)) {
            decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
        } else {
            decoder = null;
        }
        return new InputCodec(decoder);
    }

    @Bean
    OutputCodec outputCodec() {
        Decoder decoder = null;
        Encoder encoder = null;
        if (properties.enabled) {
            if (StringUtils.isNotBlank(properties.keyForDecode)) {
                decoder = CodecProvider.GenerateDecoder(properties.type, properties.keyForDecode);
            }
            if (StringUtils.isNotBlank(properties.keyForEncode)) {
                encoder = CodecProvider.GenerateEncoder(properties.type, properties.keyForEncode);
            }
        }
        return new OutputCodec(encoder, decoder);
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
