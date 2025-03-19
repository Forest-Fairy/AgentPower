package org.agentpower.agent.service;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.tool.AgentPowerChatModelDelegate;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.repo.AgentSessionRepo;
import org.agentpower.agent.repo.ChatMessageRepo;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.Constants;
import org.agentpower.api.FunctionRequest;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.service.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class AgentChatService {
    private final ConfigurationService configurationService;
    private final VectorStore vectorStore;
    private final Neo4jClient neo4jClient;
    private final ChatMessageRepo chatMessageRepo;
    private final AgentSessionRepo agentSessionRepo;
    private final AgentPowerChatMemory chatMemory;

    public Flux<ServerSentEvent<String>> chat(ChatMessageModel messageModel) {
        String requestId = messageModel.getRequestId();
        AgentModelConfiguration agentModelConfiguration = configurationService.getAgentModelConfiguration(messageModel.getAgentModelConfigurationId());
        AgentPowerChatModelDelegate chatModel = new AgentPowerChatModelDelegate(requestId, agentModelConfiguration);
        boolean hasClientService = StringUtils.isNotBlank(messageModel.getClientAgentServiceConfigurationId());
        Map<String, AgentPowerFunctionDefinition> functions;
        if (hasClientService) {
            ClientServiceConfiguration clientServiceConfiguration = configurationService
                    .getClientServiceConfiguration(messageModel.getClientAgentServiceConfigurationId());
            functions = AgentChatHelper.Prompt.getFunctions(configurationService, requestId, Globals.User.getLoginUser(), clientServiceConfiguration);
            AgentChatHelper.Registry.startConversation(requestId, messageModel, chatModel, clientServiceConfiguration, functions);
        } else {
            functions = Map.of();
        }
        int chatMemoryCouplesCount = messageModel.getChatMemoryCouplesCount() != 0 ? messageModel.getChatMemoryCouplesCount() :
                Optional.ofNullable(agentModelConfiguration.getChatMemoryCouplesCount()).orElse(5);
        return prompt(chatModel, messageModel, functions.keySet(), chatMemoryCouplesCount)
                .map(chatResponse -> ServerSentEvent.builder(JSON.toJSONString(chatResponse))
                        .event(Constants.Event.AGENT_CALL)
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
                .tools(functionNames.stream().map(name -> AgentChatHelper.FunctionInfo.wrapFunctionName(
                        messageModel.getRequestId(), messageModel.getClientAgentServiceConfigurationId(), name))
                        .toList().toArray(new String[0]))
                // 先尝试不传入toolContext
//                .toolContext(Map.of("requestId", requestId))
                .advisors(advisorSpec -> {
                    // 历史记录
                    memory(messageModel, chatMemoryCouplesCount, advisorSpec);
                    // 向量检索增强
                    rag(advisorSpec, messageModel);
                    // 知识图谱增强检索
                    graph(advisorSpec, messageModel);
                })
                .stream()
                .chatResponse();
    }

    private void memory(ChatMessageModel messageModel, int chatMemoryCouplesCount, ChatClient.AdvisorSpec advisorSpec) {
        advisorSpec.advisors(new MessageChatMemoryAdvisor(chatMemory, messageModel.getSessionId(), chatMemoryCouplesCount * 2));
    }

    private void rag(ChatClient.AdvisorSpec advisorSpec, ChatMessageModel messageModel) {
        Optional.ofNullable(messageModel.getKnowledgeBaseId()).filter(StringUtils::isNotBlank)
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
    }

    private void graph(ChatClient.AdvisorSpec advisorSpec, ChatMessageModel messageModel) {
//        Optional.ofNullable(messageModel.getKnowledgeBaseId()).filter(StringUtils::isNotBlank)
//                .ifPresent(knowledgeBaseId -> {
//                    String promptWithContext = """
//                            以下可能有可供参考的信息
//                            ---start---
//                            {question_answer_context}
//                            ---end---
//                            """;
//                    advisorSpec.advisors(new QuestionAnswerAdvisor(
//                            vectorStore,
//                            SearchRequest.builder()
//                                    .filterExpression(new Filter.Expression(
//                                            Filter.ExpressionType.EQ,
//                                            new Filter.Key("knowledgeBaseId"),
//                                            new Filter.Value(knowledgeBaseId)))
//                                    .build(),
//                            promptWithContext));
//                });

//        EmbeddingModel
//        List<Double> embed = ChunkController.floatsToDoubles(embeddingModel.embed(query));
//        String result = neo4jClient.query("""
//                        CALL db.index.vector.queryNodes('form_10k_chunks', 1, $embedding)
//                        yield node, score
//                        match window=(:Chunk)-[:NEXT*0..1]->(node)-[:NEXT*0..1]->(:Chunk)
//                        with nodes(window) as chunkList, node, score
//                        unwind chunkList as chunkRows
//                        with collect(chunkRows.text) as textList, node, score
//                        return apoc.text.join(textList, " \\n ")
//                        """)
//                .bind(embed).to("embedding")
//                .fetchAs(String.class).first()
//                .orElseThrow(() -> new BusinessException("未找到相似文档"));
//        String content = promptTemplate.createMessage(Map.of("question_answer_context", result)).getContent();
//        return chatModel.call(new UserMessage(content + "\n" + query));
    }

}
