package org.agentpower.agent.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.api.message.ChatMessageObject;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.configuration.agent.provider.AgentModelProvider;
import org.agentpower.agent.service.AgentChatService;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.agentpower.service.Globals;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/agent")
@AllArgsConstructor
public class AgentController {
    private final AgentChatService chatService;

    /**
     * @return 支持的平台列表
     */
    @GetMapping("platforms")
    public List<String> platforms() {
        return AgentModelProvider.getPlatforms();
    }

    /**
     * @return 支持的资源类型
     */
    @GetMapping("resource/providers")
    public List<Map<String, String>> resourceProviders() {
        return ResourceProvider.providers()
                .stream().map(p -> Map.of(
                        "type", p.type(),
                        "desc", p.desc()
                )).toList();
    }

    /**
     * 文本聊天接口
     * @param messageObject 聊天消息
     * @param file 系统知识文件
     * @return 消息流
     */
    @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestPart ChatMessageObject messageObject,
                                              @RequestPart(required = false) MultipartFile file) throws IOException {
        String loginUserId = Globals.User.getLoginUser().getId();
        String systemKnowledge = getKnowledgeFromFile(file);
        return Optional.of(ChatMessageModel.builder())
                .map(builder -> builder.requestId(Globals.RequestContext.getRequestId())
                        .messageType(MessageType.USER.getValue())
                        .sessionId(messageObject.sessionId())
                        .textContent(messageObject.textContent())
                        .systemKnowledge(systemKnowledge)
                        .agentModelConfigurationId(messageObject.setting().clientAgentModelId())
                        .clientAgentServiceConfigurationId(messageObject.setting().clientAgentServiceId())
                        .knowledgeBaseId(messageObject.setting().knowledgeBaseId())
                        .chatMemoryCouplesCount(messageObject.setting().chatMemoryCouplesCount())
                        .resourceProviders(JSON.toJSONString(messageObject.setting().resourceProviders()))
                        .userId(loginUserId)
                        .createdBy(loginUserId)
                        .createdTime(DateUtil.now())
                        .build())
                .map(chatService::chat)
                .orElse(Flux.empty());
    }

    private String getKnowledgeFromFile(MultipartFile file) throws IOException {
        if (file == null) {
            return null;
        }
        return new TikaDocumentReader(new InputStreamResource(file.getInputStream())).get().get(0).getText();
    }

}
