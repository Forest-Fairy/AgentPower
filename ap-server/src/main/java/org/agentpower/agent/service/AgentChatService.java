package org.agentpower.agent.service;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.tool.AgentPowerChatModelDelegate;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.repo.AgentSessionRepo;
import org.agentpower.agent.repo.ChatMessageRepo;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
@AllArgsConstructor
public class AgentChatService {
    private final ConfigurationService configurationService;
    private final VectorStore vectorStore;
    private final ChatMessageRepo chatMessageRepo;
    private final AgentSessionRepo agentSessionRepo;
    private final AgentPowerChatMemory chatMemory;

    public Flux<ServerSentEvent<String>> chat(ChatMessageModel messageModel) {
        String requestId = messageModel.getRequestId();
        AgentModelConfiguration agentModelConfiguration = configurationService.getAgentModelConfiguration(messageModel.getAgentModelConfigurationId());
        AgentPowerChatModelDelegate chatModel = new AgentPowerChatModelDelegate(requestId, agentModelConfiguration);
        boolean hasClientService = StringUtils.isNotBlank(messageModel.getClientAgentServiceConfigurationId());
        Map<String, AgentPowerFunction> functions;
        if (hasClientService) {
            ClientServiceConfiguration clientServiceConfiguration = configurationService
                    .getClientServiceConfiguration(messageModel.getClientAgentServiceConfigurationId());
            functions = AgentChatHelper.Prompt.getFunctions(requestId, messageModel.getCreatedBy(), clientServiceConfiguration);
            AgentChatHelper.Registry.startConversation(requestId, messageModel, chatModel, clientServiceConfiguration, functions);
        } else {
            functions = Map.of();
        }
        int chatMemoryCouplesCount = messageModel.getChatMemoryCouplesCount() != 0 ? messageModel.getChatMemoryCouplesCount() :
                Optional.ofNullable(agentModelConfiguration.getChatMemoryCouplesCount()).orElse(5);
        return prompt(chatModel, messageModel, functions.keySet(), chatMemoryCouplesCount)
                .map(chatResponse -> ServerSentEvent.builder(JSON.toJSONString(chatResponse))
                        .event(FunctionRequest.Event.AGENT_CALL)
                        .build())
                .doAfterTerminate(() -> AgentChatHelper.Registry.endConversation(requestId));
    }

    public Flux<ChatResponse> prompt(AgentPowerChatModelDelegate chatModel,
                                     ChatMessageModel messageModel,
                                     Collection<String> functionNames,
                                     int chatMemoryCouplesCount) {
        return ChatClient.create(chatModel)
                .prompt()
                // 启用文件提示词
                .system(systemSpec -> AgentChatHelper.Prompt.wrapSystemPrompt(systemSpec, messageModel))
                .user(userSpec -> AgentChatHelper.Prompt.wrapUserPrompt(userSpec, messageModel, configurationService))
                // 工具列表
                .tools(functionNames.stream().map(n -> AgentChatHelper.FunctionInfo.wrapFunctionName(
                        messageModel.getRequestId(), messageModel.getClientAgentServiceConfigurationId(), n)))
                // 先尝试不传入toolContext
//                .toolContext(Map.of("requestId", requestId))
                .advisors(advisorSpec -> {
                    // 历史记录
                    advisorSpec.advisors(new MessageChatMemoryAdvisor(chatMemory, messageModel.getSessionId(), chatMemoryCouplesCount * 2));
                    // 向量检索增强
                    Optional.of(messageModel.getKnowledgeBaseId()).filter(StringUtils::isNotBlank)
                            .ifPresent(knowledgeBaseId -> {
                                String promptWithContext = """
                                        以下可能有可供参考的信息
                                        ---start---
                                        {question_answer_context}
                                        ---end---
                                        """;
                                advisorSpec.advisors(new QuestionAnswerAdvisor(
                                        vectorStore,
                                        SearchRequest.builder()
                                                .filterExpression(new Filter.Expression(
                                                        Filter.ExpressionType.EQ,
                                                        new Filter.Key("knowledgeBaseId"),
                                                        new Filter.Value(knowledgeBaseId)))
                                                .build(),
                                        promptWithContext));
                            });
                })
                .stream()
                .chatResponse();
    }

}
