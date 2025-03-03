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
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.client.ClientServiceConfiguration;
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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
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
    @Override
    public void add(String conversationId, List<Message> messages) {

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        // conversationId == session id

        return chatMessageRepo
                // 查询会话内的最新n条消息
                .findBy(Example.of(ChatMessageModel.builder().sessionId(conversationId).build()),
                        s -> s.sortBy(Sort.by(Sort.Direction.DESC, "createdTime")).limit(lastN))
                .stream()
                .map(m -> {
                    if (m.getMessageType().equals(MessageType.USER.getValue())) {
                        return new UserMessage(m.getTextContent(), null);
                    } else if (m.getMessageType().equals(MessageType.ASSISTANT.getValue())) {
                        return new AssistantMessage(m.getTextContent(), null);
                    } else if (m.getMessageType().equals(MessageType.SYSTEM.getValue())) {
                        return new SystemMessage(m.getTextContent());
                    }
                    throw new IllegalArgumentException("不支持的消息类型: " + m.getMessageType());
                })
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        chatMessageRepo.delete(ChatMessageModel.builder().sessionId(conversationId).build());
    }

    public static Message BuildMessage(AiMessage aiMessage) {
        List<Media> mediaList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(aiMessage.medias())) {
            mediaList = aiMessage.medias().stream().map(AiMessageChatMemory::toSpringAiMedia).toList();
        }
        if (aiMessage.type().equals(MessageType.ASSISTANT)) {
            return new AssistantMessage(aiMessage.textContent());
        }
        if (aiMessage.type().equals(MessageType.USER)) {
            return new UserMessage(aiMessage.textContent(), mediaList);
        }
        if (aiMessage.type().equals(MessageType.SYSTEM)) {
            return new SystemMessage(aiMessage.textContent());
        }
        throw new BusinessException("不支持的消息类型");
    }
    public static Media toSpringAiMedia(AiMessage.Media media) {
        return new Media(new MediaType(media.getType()), new URL(media.getData()));
    }
}
