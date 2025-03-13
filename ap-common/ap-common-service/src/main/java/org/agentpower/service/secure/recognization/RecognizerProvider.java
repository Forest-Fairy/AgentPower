package org.agentpower.service.secure.recognization;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class RecognizerProvider {
    public final String[] names;
    public RecognizerProvider(String name, String... names) {
        this.names = new String[names.length + 1];
        int p = 0;
        Assert.isTrue(name == null || name.isBlank(), "供应者名称不能为空");
        Registry.register(name.toLowerCase(), this);
        this.names[p++] = name;
        for (String n : names) {
            Assert.isTrue(n == null || n.isBlank(), "供应者名称不能为空");
            Registry.register(n, this);
            this.names[p++] = n;
        }
    }

    protected abstract Recognizer generateRecognizer(String headerField, JSONObject properties);

    public static Recognizer generateRecognizer(String type, String headerField, JSONObject properties) {
        return Registry.providers.computeIfAbsent(type, (k) -> DefaultProvider.INSTANCE)
                .generateRecognizer(headerField, properties);
    }
    public static class Registry {
        private static final Map<String, RecognizerProvider> providers = new HashMap<>();
        public static void register(String type, RecognizerProvider provider) {
            RecognizerProvider old = providers.put(type, provider);
            if (old != null) {
                providers.put(type, old);
                throw new IllegalArgumentException("识别器供应者冲突：" + type);
            }
        }
    }

    private static class DefaultProvider extends RecognizerProvider {
        public static final DefaultProvider INSTANCE = new DefaultProvider();

        public DefaultProvider() {
            super("json");
        }

        @Override
        protected Recognizer generateRecognizer(String headerField, JSONObject properties) {
            return new JSONRecognizer(headerField);
        }
    }
    @Log4j2
    private static class JSONRecognizer extends Recognizer {
        public JSONRecognizer(String headerField) {
            super(headerField);
        }

        @Override
        public Optional<LoginUserVo> recognize(String token) {
            return Optional.ofNullable(token)
                    .map(t -> {
                        try {
                            return JSON.parseObject(t, LoginUserVo.class);
                        } catch (Exception e) {
                            log.error("用户信息解析失败: " + t, e);
                            return null;
                        }
                    });
        }

        @Override
        public String generateToken(LoginUserVo user) {
            return JSON.toJSONString(user);
        }
    }
}
