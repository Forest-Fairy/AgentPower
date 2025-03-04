package org.agentpower.agent.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.repo.AgentSessionRepo;
import org.agentpower.agent.repo.ChatMessageRepo;
import org.agentpower.agent.tool.AgentPowerChatModelDelegate;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.api.message.ChatMediaResourceProvider;
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.agentpower.infrastracture.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.Media;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import sun.misc.Unsafe;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AgentPowerChatMemory implements ChatMemory {
    private final ChatMessageRepo chatMessageRepo;
    private final ConfigurationService configurationService;
    @Override
    public void add(String conversationId, List<Message> messages) {

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        // conversationId == session id

        return lastN == 0 ? List.of() : chatMessageRepo
                // 查询会话内的最新n条消息
                .findBy(Example.of(ChatMessageModel.builder().sessionId(conversationId).build()),
                        s -> s.sortBy(Sort.by(Sort.Direction.DESC, "createdTime")).limit(lastN))
                .stream()
                .map(this::transMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        chatMessageRepo.delete(ChatMessageModel.builder().sessionId(conversationId).build());
    }

    public Message transMessage(ChatMessageModel messageModel) {
        List<Media> mediaList = AgentChatHelper.Prompt.extractMedia(messageModel, configurationService);
        if (messageModel.getMessageType().equals(MessageType.USER.getValue())) {
            return new UserMessage(messageModel.getTextContent(), mediaList);
        } else if (messageModel.getMessageType().equals(MessageType.ASSISTANT.getValue())) {
            return new AssistantMessage(messageModel.getTextContent(), Map.of(), List.of(), mediaList);
        } else if (messageModel.getMessageType().equals(MessageType.SYSTEM.getValue())) {
            return new SystemMessage(messageModel.getTextContent());
        }
        throw new IllegalArgumentException("不支持的消息类型: " + messageModel.getMessageType());
    }
}
