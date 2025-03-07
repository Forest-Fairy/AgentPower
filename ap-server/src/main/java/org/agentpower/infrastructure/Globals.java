package org.agentpower.infrastructure;

import com.alibaba.fastjson2.JSON;
import org.agentpower.api.FunctionRequest;
import org.agentpower.common.RSAUtil;
import org.agentpower.user.vo.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

import java.util.Map;

public class Globals {
    private Globals() {}

    public static class User {
        public static LoginUserVo getLoginUser() {
            return null;
        }
    }

    public static class Client {
        public static void sendMessage(String requestId, String event, String data) {
            ServerSentEvent<?> sse = ServerSentEvent.builder().event(event).data(data).build();
            // TODO 发送到服务端
        }
    }

    public static class RequestContext {
        public static String getRequestId() {
            return null;
        }
    }
}
