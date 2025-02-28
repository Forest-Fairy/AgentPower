package org.agentpower.infrastracture;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.agentpower.user.vo.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

import java.nio.charset.StandardCharsets;

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
