package org.agentpower.agent.service;

import org.agentpower.agent.model.ChatMessageModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AgentChatService {

    public Flux<ServerSentEvent<String>> chat(ChatMessageModel messageModel) {
        String messageId = messageModel.getId();

        return ChatClient.create(getChatModel(messageModel))
                .prompt()
                // 启用文件问答
                .system(promptSystemSpec -> wrapSystemPrompt(promptSystemSpec, messageModel))
                .user(promptUserSpec -> toPrompt(promptUserSpec, aiMessageWrapper.getMessage()))
                // agent列表
                .functions(functionBeanNames)
                .advisors(advisorSpec -> {
                    // 使用历史消息
                    useChatHistory(advisorSpec, aiMessageWrapper.getMessage().getSessionId());
                    // 使用向量数据库
                    useVectorStore(advisorSpec, aiMessageWrapper.getParams().getEnableVectorStore());
                })
                .stream()
                .chatResponse()
                .map(chatResponse -> ServerSentEvent.builder(toJson(chatResponse))
                        // 和前端监听的事件相对应
                        .event("message")
                        .build())
                .doAfterTerminate(() -> RESPONSE_MAP.remove(uuid));
    }


    public ChatModel getChatModel(ChatMessageModel messageModel) {
    }
}
