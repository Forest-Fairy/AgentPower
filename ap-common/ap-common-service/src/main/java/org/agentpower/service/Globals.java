package org.agentpower.service;

import jakarta.servlet.http.HttpServletRequest;
import org.agentpower.service.secure.SecureServiceImpl;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

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
        public static HttpServletRequest getRequest() {
            return SecureServiceImpl.getRequest();
        }
        public static String getRequestId() {
            return null;
        }
    }
}
