package org.agentpower.common;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SignAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class RSAUtil {
    public static final String ALGORITHM = SignAlgorithm.SHA512withRSA.getValue();

    public static String encrypt(String data, PublicKey publicKey) {
        return new RSA(publicKey.getAlgorithm(), null, publicKey)
                .encryptBase64(data.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);
    }

    public static String decrypt(String data, PrivateKey privateKey) {
        return new RSA(privateKey.getAlgorithm(), privateKey, null)
                .decryptStr(data, KeyType.PrivateKey);
    }

    public static KeyPair generateKeyPair(String algorithm) {
        return KeyUtil.generateKeyPair(algorithm);
    }

    public static PrivateKey generatePrivateKey(String algorithm, byte[] priKey) {
        return KeyUtil.generatePrivateKey(algorithm, priKey);
    }

    public static PrivateKey generatePrivateKey(String algorithm, String priKey) {
        return generatePrivateKey(algorithm, Base64.getDecoder().decode(priKey));
    }

    public static PublicKey generatePublicKey(String algorithm, byte[] pubKey) {
        return KeyUtil.generatePublicKey(algorithm, pubKey);
    }

    public static PublicKey generatePublicKey(String algorithm, String pubKey) {
        return generatePublicKey(algorithm, Base64.getDecoder().decode(pubKey));
    }

}