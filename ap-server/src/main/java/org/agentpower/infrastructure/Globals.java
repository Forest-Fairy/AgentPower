package org.agentpower.infrastructure;

import org.agentpower.user.vo.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

public class Globals {
    private Globals() {}

    public static class User {
        public static LoginUserVo getLoginUser() {
            return null;
        }
    }

    public static class Client {
        public static void sendMessage(String requestId, ServerSentEvent<?> event) {

        }
    }

    public static class RequestContext {
        public static String getRequestId() {
            return null;
        }
    }
}
