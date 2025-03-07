package org.agentpower.common;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

public class RSAUtil {
    public static final String ALGORITHM = "RSA";

    public static String encrypt(String algorithm, String data, String pubKey) {
        return new RSA(algorithm, null, pubKey)
                .encryptBase64(data.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
    }

    public static String decrypt(String algorithm, String data, String priKey) {
        return new RSA(algorithm, priKey, null)
                .decryptStr(data, KeyType.PrivateKey);
    }

    public static KeyPair generateKeyPair(String algorithm) {
        return KeyUtil.generateKeyPair(algorithm);
    }

}