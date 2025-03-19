package org.agentpower.service.secure.recognization;


import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.agentpower.api.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AgentPowerRecognizerConfiguration.AgentPowerRecognizerProperties.class
})
public class AgentPowerRecognizerConfiguration {
    @Resource
    private AgentPowerRecognizerProperties recognizerProperties;

    @ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "recognizer")
    static class AgentPowerRecognizerProperties implements InitializingBean {
        private boolean enabled;
        private String type;
        private String headerFields;
        private JSONObject properties;

        @Override
        public void afterPropertiesSet() throws Exception {
            this.headerFields = StringUtils.isNotBlank(headerFields) ? headerFields
                    : Constants.DEFAULT_RECOGNIZER_HEADER_FIELD;
        }
    }

    @Bean
    Recognizer recognizer() {
        return RecognizerProvider.generateRecognizer(recognizerProperties.type, recognizerProperties.headerFields, recognizerProperties.properties);
    }
}
