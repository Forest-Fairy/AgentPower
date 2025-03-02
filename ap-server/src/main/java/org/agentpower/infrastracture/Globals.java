package org.agentpower.infrastracture;

import org.agentpower.agent.tool.AgentPowerToolCallback;
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
        public static AgentPowerToolCallback getToolCallback(String clientServiceConfigId, String functionName) {
            // TODO get tool callback with requestId, service config id and function name
            //      but before that we should register all the client functions before sending prompt
            return null;
        }
        public static void sendMessage(String requestId, ServerSentEvent<?> event) {

        }
    }

    public static class RequestContext {
        public static String getRequestId() {
            return null;
        }
    }
}
