package org.agentpower.agent.service;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.agent.dto.ChatMediaResource;
import org.agentpower.agent.dto.ChatMediaResourceProvider;
import org.agentpower.agent.model.AgentSessionModel;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.repo.AgentSessionRepo;
import org.agentpower.agent.repo.ChatMessageRepo;
import org.agentpower.configuration.platform.provider.PlatformProvider;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.agentpower.infrastracture.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.Media;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@AllArgsConstructor
public class AgentChatService implements ChatMemory {
    private final ConfigurationService configurationService;
    private final VectorStore vectorStore;
    private final ChatMessageRepo chatMessageRepo;
    private final AgentSessionRepo agentSessionRepo;

    public Flux<ServerSentEvent<String>> chat(ChatMessageModel messageModel) {
        String requestId = messageModel.getRequestId();
        ClientServiceConfiguration clientServiceConfiguration = configurationService.getClientServiceConfiguration(messageModel.getClientAgentServiceConfigurationId());
        AgentModelConfiguration agentModelConfiguration = configurationService.getAgentModelConfiguration(messageModel.getAgentModelConfigurationId());
        List<AgentPowerFunction> functions = getFunctions(requestId, clientServiceConfiguration);
        return ChatClient.create(PlatformProvider.GetModel(agentModelConfiguration))
                .prompt()
                // 启用文件提示词
                .system(systemSpec -> this.wrapSystemPrompt(systemSpec, messageModel))
                .user(userSpec -> this.wrapUserPrompt(userSpec, messageModel))
                // 工具列表
                .tools(functions.stream().map(AgentPowerFunction::functionName).toList().toArray(new String[0]))
                // 先尝试不传入toolContext
//                .toolContext(Map.of("requestId", requestId))
                .advisors(advisorSpec -> {
                    // 历史记录
                    advisorSpec.advisors(new MessageChatMemoryAdvisor(this, messageModel.getSessionId(),
                            Optional.ofNullable(agentModelConfiguration.getChatMemoryCount()).map(i -> i*2).orElse(10)));
                    // 向量检索增强
                    Optional.of(messageModel.isEnableVectorStore()).filter(b -> b)
                                    .ifPresent(b -> {
                                        String promptWithContext = """
                                            以下是可供参考的上下文信息
                                            ---start---
                                            {question_answer_context}
                                            ---end---
                                            """;
                                            advisorSpec.advisors(new QuestionAnswerAdvisor(vectorStore,
                                                    SearchRequest.builder().build(), promptWithContext));
                                    });
                })
                .stream()
                .chatResponse()
                .map(chatResponse -> ServerSentEvent.builder(JSON.toJSONString(chatResponse))
                        // 和前端监听的事件相对应
                        .event("message")
                        .build())
                .doAfterTerminate(() -> {});
    }

    private void wrapUserPrompt(ChatClient.PromptUserSpec promptUserSpec, ChatMessageModel messageModel) {
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

    private void wrapSystemPrompt(ChatClient.PromptSystemSpec promptSystemSpec, ChatMessageModel messageModel) {
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

    private static final Map<String, Object> FUNCTIONS_CACHE = new ConcurrentHashMap<>();
    private static List<AgentPowerFunction> getFunctions(String requestId, ClientServiceConfiguration clientServiceConfiguration) {
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
                                        "auth", RSAUtil.encrypt(
                                                "RSA",
                                                Globals.User.getLoginUser().getId(),
                                                clientServiceConfiguration.getServicePublicKey()))
                        )))
                .build());
        String functionDefinitionKey = requestId;
        try {
            return CompletableFuture.supplyAsync(() -> {
                while (!FUNCTIONS_CACHE.containsKey(functionDefinitionKey)) {
                    Unsafe.getUnsafe().park(true, TimeUnit.MILLISECONDS.toNanos(500));
                }
                return (List<AgentPowerFunction>) FUNCTIONS_CACHE.remove(functionDefinitionKey);
            }).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            FUNCTIONS_CACHE.put(functionDefinitionKey, StatusCode.REQUEST_ABORT);
            throw new RuntimeException(e);
        }
    }
    public static int receiveFunctionList(String requestId, List<AgentPowerFunction> functions) {
        String functionDefinitionKey = requestId;
        if (FUNCTIONS_CACHE.containsKey(functionDefinitionKey)) {
            return Integer.parseInt(FUNCTIONS_CACHE.get(functionDefinitionKey).toString());
        } else {
            FUNCTIONS_CACHE.put(functionDefinitionKey, functions);
            return StatusCode.OK;
        }
    }
    public static AgentPowerFunction getFunctionDefinition(String requestId, String functionName) {
        return Optional.of(FUNCTIONS_CACHE.get(requestId)).stream()
                .filter(f -> f instanceof List)
                .map(f -> (List<?>) f)
                .flatMap(List::stream)
                .map(f -> (AgentPowerFunction) f)
                .filter(f -> f.functionName().equals(functionName))
                .findFirst().orElse(null);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        return List.of();
    }

    @Override
    public void clear(String conversationId) {

    }
}
