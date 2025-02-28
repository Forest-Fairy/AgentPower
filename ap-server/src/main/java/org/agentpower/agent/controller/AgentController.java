package org.agentpower.agent.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.service.AgentChatService;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agent")
@AllArgsConstructor
public class AgentController {
    private final AgentChatService chatService;

    /**
     * 发起会话接口
     * @param params                消息参数
     * @return 消息流
     */
    @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestPart String params,
                                              @RequestPart(required = false) MultipartFile file) throws IOException {
        MessageObject messageObject = JSON.parseObject(params, MessageObject.class);
        String uuid = UUID.fastUUID().toString();
        String systemKnowledge = getKnowledgeFromFile(file);
        List<Provider> providers = messageObject.setting().resourceProviders() == null ?
                List.of() : messageObject.setting().resourceProviders();
        return Optional.of(ChatMessageModel.builder())
                .map(builder -> builder.id(uuid)
                        .sessionId(messageObject.sessionId())
                        .question(messageObject.question())
                        .systemKnowledge(systemKnowledge)
                        .enableVectorStore(messageObject.setting().enableVectorStore())
                        .clientAgentServiceId(messageObject.setting().clientAgentServiceId())
                        .resourceProviders(JSON.toJSONString(providers))
                        .userId(Globals.User.getLoginUser().getId())
                        .createdBy(Globals.User.getLoginUser().getId())
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

    /**
     * 消息体对象
     * @param sessionId             会话id
     * @param question              问题内容
     */
    public record MessageObject(MessageSetting setting, String sessionId, String question) {}

    /**
     *
     * @param enableVectorStore     启用向量数据库
     * @param clientAgentServiceId  客户端代理增强配置id -> 空表示无启用
     * @param resourceProviders     资源提供者列表
     */
    public record MessageSetting(boolean enableVectorStore, String clientAgentServiceId, List<Provider> resourceProviders) {}

    /**
     * 资源提供者
     * @param configId              资源提供者配置id
     * @param mediaList             多媒体资源列表
     */
    public record Provider(String configId, List<MessageMedia> mediaList) {}

    /**
     * 多媒体资源
     * @param id                    资源id
     * @param mediaType             资源类型 -> 转换为 spring ai 的类型
     */
    public record MessageMedia(String id, String mediaType) {}
}
