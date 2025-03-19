package org.agentpower.configuration;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import org.agentpower.api.Constants;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.agent.AgentModelConfigurationRepo;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.client.ClientServiceConfigurationRepo;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.ResourceProviderConfigurationRepo;
import org.agentpower.service.secure.codec.AgentPowerCodecConfiguration;
import org.agentpower.service.secure.codec.CodecProvider;
import org.agentpower.service.secure.codec.Encoder;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.Recognizer;
import org.agentpower.service.secure.recognization.RecognizerProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConfigurationService {
    private final ClientServiceConfigurationRepo clientServiceConfigurationRepo;
    private final AgentModelConfigurationRepo agentModelConfigurationRepo;
    private final ResourceProviderConfigurationRepo resourceProviderConfigurationRepo;
    private final AgentPowerCodecConfiguration codecConfiguration;

    public ClientServiceConfiguration getClientServiceConfiguration(String clientServiceConfigId) {
        return clientServiceConfigurationRepo.findById(clientServiceConfigId)
                .orElse(JSON.parseObject("{'id': '" + clientServiceConfigId + "', 'serviceUrl': 'unknown'}",
                        ClientServiceConfiguration.class));
    }

    public AgentModelConfiguration getAgentModelConfiguration(String agentModelConfigId) {
        return agentModelConfigurationRepo.findById(agentModelConfigId)
                .orElse(JSON.parseObject("{'id': '" + agentModelConfigId + "', 'agentPlatform': 'unknown'}",
                        AgentModelConfiguration.class));
    }

    public ResourceProviderConfiguration getResourceProviderConfiguration(String resourceProviderConfigId) {
        return resourceProviderConfigurationRepo.findById(resourceProviderConfigId)
                .orElse(JSON.parseObject("{'id': '" + resourceProviderConfigId + "', 'type': 'unknown'}",
                        ResourceProviderConfiguration.class));
    }

    public Map<String, Object> buildClientServiceHeader(
            ClientServiceConfiguration clientServiceConfiguration, LoginUserVo loginUser) {
        JSONObject header = JSON.parseObject(clientServiceConfiguration.getHeaders());
        header = header == null ? new JSONObject() : header;
        header.put(Constants.Header.ALGORITHM, codecConfiguration.codecAlgorithm());
        header.put(Constants.Header.ENCODED_KEY, codecConfiguration.keyForEncode());
        header.put(clientServiceConfiguration.getRecognizerHeaderField(),
                generateAuthorization(clientServiceConfiguration, loginUser));
        return Map.copyOf(header);
    }

    public Map<String, Object> buildClientServiceBody(ClientServiceConfiguration clientServiceConfiguration, Map<String, Object> params) {
        Map<String, Object> body = params == null ? new HashMap<>() : new HashMap<>(params);
        body.put(Constants.Body.CLIENT_CONFIGURATION_ID, clientServiceConfiguration.getId());
        body.put(Constants.Body.SERVICE_URL, clientServiceConfiguration.getServiceUrl());
        return Map.copyOf(body);
    }

    private String generateAuthorization(ClientServiceConfiguration clientServiceConfiguration, LoginUserVo user) {
        Recognizer recognizer = RecognizerProvider.generateRecognizer(
                clientServiceConfiguration.getRecognizerType(),
                clientServiceConfiguration.getRecognizerHeaderField(),
                JSON.parseObject(clientServiceConfiguration.getRecognizerProperties()));
        String auth = recognizer.generateToken(user);
        String keyForEncode = clientServiceConfiguration.getServiceKeyForEncode();
        if (keyForEncode != null) {
            Encoder encoder = CodecProvider.GenerateEncoder(
                    clientServiceConfiguration.getServiceAlgorithm(), keyForEncode);
            auth = encoder.encodeToBase64Str(auth);
        }
        return auth;
    }
}
