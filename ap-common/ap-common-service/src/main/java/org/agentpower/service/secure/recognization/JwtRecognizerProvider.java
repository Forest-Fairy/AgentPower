package org.agentpower.service.secure.recognization;

import cn.hutool.jwt.JWT;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.agentpower.common.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "agent-power.server", name = "jwt-secret")
public class JwtRecognizerProvider extends RecognizerProvider {

    public JwtRecognizerProvider() {
        super("jwt");
    }

    @Override
    protected Recognizer generateRecognizer(String headerField, JSONObject properties) {
        return new JwtRecognizer(headerField, properties.getString("secret"));
    }

    public static class JwtRecognizer extends Recognizer {
        private final byte[] secret;

        public JwtRecognizer(String headerField, String secret) {
            super(headerField);
            this.secret = Base64.getDecoder().decode(secret);
        }

        @Override
        public Optional<LoginUserVo> recognize(String token) {
            JWT jwt = JwtUtil.parseJWT(token);
            if (! JwtUtil.isValid(jwt, secret) ||
                JwtUtil.isExpired(jwt, 30)) {
                return Optional.empty();
            }
            return Optional.of(JSON.parseObject(
                    JSON.toJSONString(jwt.getPayload(JWT.SUBJECT)),
                    LoginUserVo.class));
        }

        @Override
        public String generateToken(LoginUserVo user) {
            return "";
        }
    }
}
