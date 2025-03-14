package org.agentpower.service.secure.recognization.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.agentpower.api.Constants;
import org.agentpower.common.JwtUtil;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.Recognizer;
import org.agentpower.service.secure.recognization.RecognizerProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(
        prefix = Constants.CONFIG_PREFIX + "." + "recognizer",
        name = "type", havingValue = "jwt")
public class JwtRecognizerProvider extends RecognizerProvider {

    public JwtRecognizerProvider() {
        super("jwt");
    }

    @Override
    protected Recognizer generateRecognizer(String headerField, JSONObject properties) {
        return new JwtRecognizer(headerField, properties.getString("secret"),
                properties.getString("issuer"), properties.getLong("expiration"));
    }

    public static class JwtRecognizer extends Recognizer {
        private final byte[] secret;
        private final String issuer;
        private final long expiration;
        public JwtRecognizer(String headerField, String secret,
                             String issuer, Long expiration) {
            super(headerField);
            this.secret = Base64.getDecoder().decode(secret);
            this.issuer = issuer == null ? "agent-power" : issuer;
            this.expiration = (expiration == null ? 60 * 60 * 24 * 7 : expiration) * 1000;
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
            return JwtUtil.createJWT(UUID.fastUUID().toString(),
                    JSON.toJSONString(user),
                    issuer, expiration,
                    Map.of(), secret);
        }
    }
}
