package org.agentpower.agent;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.agentpower.agent.tool.AgentPowerChatModelDelegate;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.api.*;
import org.agentpower.api.message.ChatMediaResource;
import org.agentpower.api.message.ChatMediaResourceProvider;
import org.agentpower.common.Tuples;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.agentpower.service.Globals;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import sun.misc.Unsafe;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Log4j2
public class AgentChatHelper {
    private AgentChatHelper() {}
    private static final Map<String, ChatRuntime> CHAT_RUNTIME_CACHE = new ConcurrentHashMap<>();

    public static class Registry {
        private Registry() {}

        public static void startConversation(String requestId, ChatMessageModel messageModel, AgentPowerChatModelDelegate chatModel,
                                             ClientServiceConfiguration clientServiceConfiguration, Map<String, AgentPowerFunctionDefinition> functions) {
            // TODO 校验空
            ChatRuntime runtime = new ChatRuntime(messageModel, chatModel);
            runtime.cacheClientFunctions(clientServiceConfiguration, functions);
            CHAT_RUNTIME_CACHE.put(requestId, runtime);
        }

        public static void updateConversationCache(String requestId, ClientServiceConfiguration clientServiceConfiguration, Map<String, AgentPowerFunctionDefinition> functions) {
            ChatRuntime runtime = CHAT_RUNTIME_CACHE.get(requestId);
            if (runtime == null) {
                throw new IllegalStateException("会话已终止");
            }
            runtime.cacheClientFunctions(clientServiceConfiguration, functions);
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

        public static void wrapUserPrompt(
                ChatClient.PromptUserSpec promptUserSpec, ChatMessageModel messageModel,
                ConfigurationService configurationService) {
            String textContent = messageModel.getTextContent();
            if (textContent != null) {
                promptUserSpec.text(textContent);
            }
            List<Media> mediaList = extractMedia(messageModel, configurationService);
            if (mediaList.isEmpty()) return;
            promptUserSpec.media(mediaList.toArray(new Media[0]));
        }

        public static List<Media> extractMedia(ChatMessageModel messageModel, ConfigurationService configurationService) {
            List<ChatMediaResourceProvider> providers = JSON.parseArray(
                    messageModel.getResourceProviders(), ChatMediaResourceProvider.class);
            if (providers == null || providers.isEmpty()) {
                return List.of();
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
                    ResourceProvider resourceProvider = ResourceProvider.getProvider(resourceProviderConfiguration.getType());
                    for (ChatMediaResource mediaResource : provider.mediaList()) {
                        mediaList.add(resourceProvider.getSource(resourceProviderConfiguration, mediaResource.id()));
                    }
                }
            }
            return mediaList;
        }


        private static final Map<String, Object> FUNCTIONS_CACHE = new ConcurrentHashMap<>();
        public static Map<String, AgentPowerFunctionDefinition> getFunctions(ConfigurationService configurationService, String requestId, LoginUserVo user, ClientServiceConfiguration clientServiceConfiguration) {
            if (clientServiceConfiguration == null) {
                // 未启用智能体
                return Map.of();
            }
            Map<String, AgentPowerFunctionDefinition> functionMap = Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(runtime -> runtime.getClients().get(clientServiceConfiguration.getId()))
                    .map(Tuples._2::t2)
                    .orElse(null);
            if (functionMap != null) {
                // 从缓存获取到了 如果是空集合 表示客户端没有智能体函数
                return functionMap;
            }
            Map<String, Object> header = configurationService.buildClientServiceHeader(
                    clientServiceConfiguration, user);
            Map<String, Object> body = configurationService.buildClientServiceBody(
                    clientServiceConfiguration, null);
            try {
                Globals.Client.sendMessage(requestId, Constants.Event.LIST_FUNCTIONS,
                        JSON.toJSONString(new FunctionRequest(requestId, Constants.Event.LIST_FUNCTIONS,
                                FunctionRequest.Header.of(header), FunctionRequest.Body.of(body))));
            } catch (IOException e) {
                Globals.Client.trySendMessage(requestId, Constants.Event.ALERT_FAILED, "客户端增强服务获取方法集失败");
                log.error(requestId + " 发送消息给客户端出错", e);
            }
            String functionDefinitionKey = requestId;
            try {
                String functionsContent = CompletableFuture.supplyAsync(() -> {
                    Object f;
                    while ((f = FUNCTIONS_CACHE.remove(functionDefinitionKey)) == null) {
                        Unsafe.getUnsafe().park(true, TimeUnit.MILLISECONDS.toNanos(500));
                    }
                    return (String) f;
                }).get(30, TimeUnit.SECONDS);
                AgentPowerServer.FunctionListResult functionListResult = JSON.parseObject(functionsContent, AgentPowerServer.FunctionListResult.class);
                if (StringUtils.isNotBlank(functionListResult.errorInfo())) {
                    // 获取失败
                    throw new IllegalStateException("函数列表获取失败：" + functionListResult.errorInfo());
                }
                List<AgentPowerFunctionDefinition> functions = functionListResult.functions();
                return functions.stream().collect(Collectors.toMap(AgentPowerFunctionDefinition::functionName, func -> func, (o1, o2) -> o2));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                FUNCTIONS_CACHE.put(functionDefinitionKey, StatusCode.REQUEST_ABORT);
                throw new RuntimeException(e);
            }
        }
        public static int receiveFunctionList(String requestId, String content) {
            String functionDefinitionKey = requestId;
            Object old = FUNCTIONS_CACHE.put(functionDefinitionKey, content);
            if (old instanceof Integer abort) {
                FUNCTIONS_CACHE.remove(functionDefinitionKey);
                return abort;
            }
            return StatusCode.OK;
        }

    }

    public static class Runtime {
        private Runtime() {}
        public static AgentPowerFunctionDefinition getFunctionDefinition(String requestId, String clientServiceId, String functionName) {
            return Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(ChatRuntime::getClients)
                    .map(cs -> cs.get(clientServiceId).t2())
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
        public static AgentPowerChatModelDelegate getChatModelDelegate(String requestId, String modelConfigId) {
            return Optional.ofNullable(CHAT_RUNTIME_CACHE.get(requestId))
                    .map(ChatRuntime::getModels)
                    .map(models -> models.get(modelConfigId))
                    .map(Tuples._2::t2)
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
        Map<String, Tuples._2<AgentModelConfiguration, AgentPowerChatModelDelegate>> models;
        Map<String, Tuples._2<ClientServiceConfiguration, Map<String, AgentPowerFunctionDefinition>>> clients;

        public ChatRuntime(ChatMessageModel messageModel, AgentPowerChatModelDelegate chatModel) {
            this.messageModel = messageModel;
            this.chatModel = chatModel;
            this.models = new ConcurrentHashMap<>();
            this.clients = new ConcurrentHashMap<>();
        }

        void cacheClientFunctions(ClientServiceConfiguration configuration, Map<String, AgentPowerFunctionDefinition> functions) {
            this.clients.put(configuration.getId(), new Tuples._2<>(configuration, functions));
        }

        void cacheModel(AgentModelConfiguration configuration, AgentPowerChatModelDelegate model) {
            this.models.put(configuration.getId(), new Tuples._2<>(configuration, model));
        }

    }

}
