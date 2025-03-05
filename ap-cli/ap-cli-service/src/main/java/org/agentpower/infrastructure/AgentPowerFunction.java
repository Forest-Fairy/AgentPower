package org.agentpower.infrastructure;

import com.alibaba.fastjson2.JSON;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.message.ChatMessageObject;

public interface AgentPowerFunction {
    default String wrapFunctionResult(String result) {
        return toJSONString(new FunctionRequest.CallResult(
                FunctionRequest.CallResult.Type.DIRECT, result));
    }
    default String wrapAgentCall(ChatMessageObject messageObject) {
        return toJSONString(new FunctionRequest.CallResult(
                FunctionRequest.CallResult.Type.AGENT, toJSONString(messageObject)));
    }
    default String wrapErrorResult(String error) {
        return toJSONString(new FunctionRequest.CallResult(
                FunctionRequest.CallResult.Type.ERROR, error));
    }
    default String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }
}
