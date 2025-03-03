package org.agentpower.api.message;

/**
 * 消息体对象
 * @param sessionId             会话id
 * @param textContent           问题内容
 */
public record ChatMessageObject(ChatMessageSetting setting, String sessionId, String textContent) {
}