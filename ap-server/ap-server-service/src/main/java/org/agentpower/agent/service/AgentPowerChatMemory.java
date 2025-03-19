package org.agentpower.agent.service;

import lombok.AllArgsConstructor;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.repo.ChatMessageRepo;
import org.agentpower.configuration.ConfigurationService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.Media;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
