package org.agentpower.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agentpower.service.secure.SecureServiceImpl;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

import java.io.IOException;

public class Globals {
    private Globals() {}

    public static class User {
        public static LoginUserVo getLoginUser() {
            return SecureServiceImpl.getLoginUser();
        }
    }

    public static class Client {
        public static void sendMessage(String requestId, String event, String data) throws IOException {
            SecureServiceImpl.sendEvent(ServerSentEvent.builder().id(requestId).event(event).data(data).build());
        }
    }

    public static class WebContext {
        public static HttpServletRequest getRequest() {
            return SecureServiceImpl.getRequest();
        }
        public static HttpServletResponse getResponse() {
            return SecureServiceImpl.getResponse();
        }
        public static String getRequestId() {
            // TODO 如果不能用 则获取 session 里的 qid
            return getRequest().getRequestId();
        }

        public static String getFullURL() {
            HttpServletRequest request = getRequest();
            String queryString = request.getQueryString();
            return queryString == null ? request.getRequestURL().toString()
                    : "%s?%s".formatted(request.getRequestURL(), queryString);
        }
    }
}
