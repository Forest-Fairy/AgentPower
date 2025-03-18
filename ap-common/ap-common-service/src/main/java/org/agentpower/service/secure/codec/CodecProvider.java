package org.agentpower.service.secure.codec;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public abstract class CodecProvider {
    public final String[] names;
    public CodecProvider(String name, String... names) {
        this.names = new String[names.length + 1];
        int p = 0;
        Assert.isTrue(name == null || name.isBlank(), "供应者名称不能为空");
        Registry.register(name, this);
        this.names[p++] = name;
        for (String n : names) {
            Assert.isTrue(n == null || n.isBlank(), "供应者名称不能为空");
            Registry.register(n, this);
            this.names[p++] = n;
        }
    }

    protected abstract Decoder generateDecoder(String algorithm, String keyForDecode);
    protected abstract Encoder generateEncoder(String algorithm, String keyForDecode);


    public static class Registry {
        private static final Map<String, CodecProvider> PROVIDER_MAP = new HashMap<>();

        public static void register(String type, CodecProvider provider) {
            CodecProvider old = PROVIDER_MAP.put(type, provider);
            if (old != null) {
                PROVIDER_MAP.put(type, old);
                throw new IllegalArgumentException("编码器供应者冲突：" + type);
            }
        }
    }

    public static Decoder GenerateDecoder(String type, String keyForDecode) {
        return Registry.PROVIDER_MAP.computeIfAbsent(type,
                        (k) -> {
                            throw new IllegalArgumentException("编码器供应者不存在：" + type);
                        })
                .generateDecoder(type, keyForDecode);
    }

    public static Encoder GenerateEncoder(String type, String keyForEncode) {
        return Registry.PROVIDER_MAP.computeIfAbsent(type,
                        (k) -> {
                            throw new IllegalArgumentException("编码器供应者不存在：" + type);
                        })
                .generateEncoder(type, keyForEncode);
    }
}
