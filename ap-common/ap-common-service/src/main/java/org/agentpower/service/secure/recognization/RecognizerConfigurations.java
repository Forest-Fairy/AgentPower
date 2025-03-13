package org.agentpower.service.secure.recognization;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import org.agentpower.api.Constants;
import org.agentpower.service.secure.codec.CodecConfigurations;
import org.agentpower.service.secure.codec.CodecProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        RecognizerConfigurations.AgentPowerRecognizerProperties.class
})
public class RecognizerConfigurations {
    @Getter
    private static Recognizer recognizer;


    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "recognizer")
    static class AgentPowerRecognizerProperties implements InitializingBean {
        private boolean enabled;
        private String type;
        private String headerFields;
        private JSONObject properties;

        @Override
        public void afterPropertiesSet() throws Exception {
            RecognizerConfigurations.recognizer = RecognizerProvider.generateRecognizer(type, headerFields, properties);
        }
    }
}
