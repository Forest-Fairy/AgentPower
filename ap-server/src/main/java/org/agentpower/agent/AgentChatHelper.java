package org.agentpower.agent;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.agentpower.agent.tool.AgentPowerChatModelDelegate;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.api.message.ChatMediaResource;
import org.agentpower.api.message.ChatMediaResourceProvider;
import org.agentpower.common.RSAUtil;
import org.agentpower.common.Tuples;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.agentpower.infrastracture.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.MimeType;
import sun.misc.Unsafe;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AgentChatHelper {
    private AgentChatHelper() {}
    private static final Map<String, ChatRuntime> CHAT_RUNTIME_CACHE = new ConcurrentHashMap<>();

    public static class Registry {
        private Registry() {}

        public static void startConversation(String requestId, ChatMessageModel messageModel, AgentPowerChatModelDelegate chatModel,
                                             ClientServiceConfiguration clientServiceConfiguration, Map<String, AgentPowerFunction> functions) {
            // TODO 校验空
            ChatRuntime runtime = new ChatRuntime(messageModel, chatModel);
            runtime.setClientFunctions(clientServiceConfiguration, functions);
            CHAT_RUNTIME_CACHE.put(requestId, runtime);
        }

        public static void updateConversationCache(String requestId, ClientServiceConfiguration clientServiceConfiguration, Map<String, AgentPowerFunction> functions) {
            ChatRuntime runtime = CHAT_RUNTIME_CACHE.get(requestId);
            if (runtime == null) {
                throw new IllegalStateException("会话已终止");
            }
            runtime.setClientFunctions(clientServiceConfiguration, functions);
        }

        public static void endConversation(String requestId) {
            CHAT_RUNTIME_CACHE.remove(requestId);
        }
    }

    public static class Prompt {
        public static void wrapSystemPrompt(ChatClient.PromptSystemSpec promptSystemSpec, ChatMessageModel messageModel) {
            String systemKnowledge = messageModel.getSystemKnowledge();
            if (systemKnowledge == null) {
                return;
            }
            promptSystemSpec.text(
                    """
                    请基于以下的知识进行回答用户的问题:
                    ---start---
                    {knowledge}
                    ---end---
                    """
            ).param("knowledge", systemKnowledge);
        }

        public static void wrapUserPrompt(ChatClient.PromptUserSpec promptUserSpec, ChatMessageModel messageModel, ConfigurationService configurationService) {
            String textContent = messageModel.getTextContent();
            if (textContent != null) {
                promptUserSpec.text(textContent);
            }
            List<ChatMediaResourceProvider> providers = JSON.parseArray(
                    messageModel.getResourceProviders(), ChatMediaResourceProvider.class);
            if (providers == null || providers.isEmpty()) {
                return;
            }
            List<Media> mediaList = new LinkedList<>();
            for (ChatMediaResourceProvider provider : providers) {
                if (StringUtils.isBlank(provider.configId())) {
                    // 直接上传原生的数据 那么可能是一段音频或一段视频
                    for (ChatMediaResource mediaResource : provider.mediaList()) {
                        mediaList.add(new Media(
                                MimeType.valueOf(mediaResource.mediaType()),
                                new ByteArrayResource(mediaResource.mediaData())));
                    }
                } else {
                    ResourceProviderConfiguration resourceProviderConfiguration = configurationService.getResourceProviderConfiguration(provider.configId());
                    ResourceProvider<Resource> resourceProvider = ResourceProvider.getProvider(resourceProviderConfiguration.getType());
                    for (ChatMediaResource mediaResource : provider.mediaList()) {
                        mediaList.add(new Media(
                                MimeType.valueOf(mediaResource.mediaType()),
                                resourceProvider.getSource(resourceProviderConfiguration, mediaResource.id())));
                    }
                }
            }
            promptUserSpec.media(mediaList.toArray(new Media[0]));
        }


        private static final Map<String, Object> FUNCTIONS_CACHE = new ConcurrentHashMap<>();
        public static Map<String, AgentPowerFunction> getFunctions(String requestId, String userId, ClientServiceConfiguration clientServiceConfiguration) {
            if (clientServiceConfiguration == null) {
                // 未启用智能体
                return Map.of();
            }
            Map<String, AgentPowerFunction> functionMap = Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(runtime -> runtime.getClients().get(clientServiceConfiguration.getId()))
                    .map(Tuples._2::t1)
                    .orElse(null);
            if (functionMap != null) {
                // 从缓存获取到了 如果是空集合 表示客户端没有智能体函数
                return functionMap;
            }
            Globals.Client.sendMessage(requestId, ServerSentEvent.builder()
                    .event(FunctionRequest.Event.LIST_FUNCTIONS)
                    .data(JSON.toJSONString(
                            new FunctionRequest(
                                    requestId,
                                    null,
                                    FunctionRequest.Event.LIST_FUNCTIONS,
                                    Map.of(
                                            "configurationId", clientServiceConfiguration.getId(),
                                            "serviceUrl", clientServiceConfiguration.getServiceUrl(),
                                            "headers", clientServiceConfiguration.getHeaders(),
                                            "auth", RSAUtil.encrypt("RSA", userId,
                                                    clientServiceConfiguration.getServicePublicKey()))
                            )))
                    .build());
            String functionDefinitionKey = requestId;
            try {
                List<AgentPowerFunction> functions = CompletableFuture.supplyAsync(() -> {
                    Object f;
                    while ((f = FUNCTIONS_CACHE.remove(functionDefinitionKey)) == null) {
                        Unsafe.getUnsafe().park(true, TimeUnit.MILLISECONDS.toNanos(500));
                    }
                    return (List<AgentPowerFunction>) f;
                }).get(30, TimeUnit.SECONDS);
                return functions.stream().collect(Collectors.toMap(AgentPowerFunction::functionName, func -> func, (o1, o2) -> o2));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                FUNCTIONS_CACHE.put(functionDefinitionKey, StatusCode.REQUEST_ABORT);
                throw new RuntimeException(e);
            }
        }
        public static int receiveFunctionList(String requestId, List<? extends AgentPowerFunction> functions) {
            String functionDefinitionKey = requestId;
            Object old = FUNCTIONS_CACHE.put(functionDefinitionKey, functions);
            if (old instanceof Integer abort) {
                FUNCTIONS_CACHE.remove(functionDefinitionKey);
                return abort;
            }
            return StatusCode.OK;
        }

    }

    public static class Runtime {
        private Runtime() {}
        public static AgentPowerFunction getFunctionDefinition(String requestId, String clientServiceId, String functionName) {
            return Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(ChatRuntime::getClients)
                    .map(cs -> cs.get(clientServiceId).t1())
                    .map(f -> f.get(functionName))
                    .orElse(null);
        }

        public static ChatMessageModel getChatMessage(String requestId) {
            return Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(ChatRuntime::getMessageModel)
                    .orElse(null);
        }

        public static AgentPowerChatModelDelegate getChatModelDelegate(String requestId) {
            return Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(ChatRuntime::getChatModel)
                    .orElse(null);
        }
    }

    public static class FunctionInfo {
        private FunctionInfo() {}
        public static class FunctionNameInfo {
            public final String requestId;
            public final String clientServiceId;
            public final String functionName;
            private FunctionNameInfo(String requestId, String clientServiceId, String functionName) {
                this.requestId = requestId;
                this.clientServiceId = clientServiceId;
                this.functionName = functionName;
            }
        }
        public static final String CALLBACK_PREFIX = "AgentPower-";
        public static String wrapFunctionName(String requestId, String clientServiceId, String functionName) {
            return String.format("%s_%s_%s_%s", CALLBACK_PREFIX, requestId, clientServiceId, functionName);
        }
        public static FunctionNameInfo unwrapFunctionName(String functionName) {
            if (! functionName.startsWith(CALLBACK_PREFIX)) {
                return null;
            }
            String[] parts = functionName.split("_", 4);
            if (parts.length != 4) {
                return null;
            }
            return new FunctionNameInfo(parts[1], parts[2], parts[3]);
        }

    }


    @Data
    private static class ChatRuntime {
        ChatMessageModel messageModel;
        AgentPowerChatModelDelegate chatModel;
        Map<String, Tuples._2<ClientServiceConfiguration, Map<String, AgentPowerFunction>>> clients;

        public ChatRuntime(ChatMessageModel messageModel, AgentPowerChatModelDelegate chatModel) {
            this.messageModel = messageModel;
            this.chatModel = chatModel;
            this.clients = new ConcurrentHashMap<>();
        }

        void setClientFunctions(ClientServiceConfiguration configuration, Map<String, AgentPowerFunction> functions) {
            this.clients.put(configuration.getId(), new Tuples._2<>(configuration, functions));
        }

    }

}
