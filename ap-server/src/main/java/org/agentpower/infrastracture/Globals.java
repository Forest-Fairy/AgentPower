package org.agentpower.infrastracture;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.agentpower.user.vo.LoginUserVo;
import org.springframework.http.codec.ServerSentEvent;

import java.nio.charset.StandardCharsets;

public class Globals {
    private Globals() {}

    public static class Const {
        public static final String TOOL_CALL = "tool-call";
    }
    public static class RSAUtil {
        public static String encrypt(String algorithm, String data, String pubKey) {
            return new RSA(algorithm, null, pubKey)
                    .encryptBase64(data.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
        }

        public static String decrypt(String algorithm, String data, String priKey) {
            return new RSA(algorithm, priKey, null)
                    .decryptStr(data, KeyType.PrivateKey);
        }

    }

    public static class User {
        public static LoginUserVo getLoginUser() {
            return null;
        }
    }

    public static class Client {
        public static void sendMessage(ServerSentEvent<?> event) {
        }
    }

    public static class RequestContext {
        public static String getRequestId() {
            return null;
        }
    }
}
