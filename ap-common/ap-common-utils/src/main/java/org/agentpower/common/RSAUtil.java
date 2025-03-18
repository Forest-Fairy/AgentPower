package org.agentpower.common;

import cn.hutool.core.annotation.Alias;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SignAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Objects;

public class RSAUtil {

    @Alias("SignAlgorithm")
    public static final Class<SignAlgorithm> algorithmClass = SignAlgorithm.class;

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

    /**
     * 需要私钥解密的直接使用系统内置的codec对象 而不是此方法
     * @param publicKey 公钥
     * @return RSA
     */
    public static RSA createToEncrypt(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "公钥不存在，请检查代码逻辑");
        return new RSA(publicKey.getAlgorithm(), null, publicKey);
    }

    public static RSA createToDecode(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "私钥不存在，请检查代码逻辑");
        return new RSA(privateKey.getAlgorithm(), privateKey, null);
    }

    public static RSA create(PublicKey publicKey, PrivateKey privateKey) {
        Objects.requireNonNull(publicKey, "公钥不存在，请检查代码逻辑");
        Objects.requireNonNull(privateKey, "私钥不存在，请检查代码逻辑");
        return new RSA(publicKey.getAlgorithm(), privateKey, publicKey);
    }

    public static byte[] encrypt(RSA rsa, byte[] data) {
        return rsa.encrypt(data, KeyType.PublicKey);
    }

    public static byte[] encryptUtf8Str(RSA rsa, String data) {
        return encrypt(rsa, data.getBytes(StandardCharsets.UTF_8));
    }

    public static String encryptUtf8Str2Base64Str(RSA rsa, String data) {
        return Base64.getEncoder().encodeToString(encryptUtf8Str(rsa, data));
    }

    public static byte[] decrypt(RSA rsa, byte[] bytes) {
        return rsa.decrypt(bytes, KeyType.PrivateKey);
    }
    public static byte[] decryptBase64Str(RSA rsa, String base64Str) {
        return decrypt(rsa, Base64.getDecoder().decode(base64Str));
    }

    public static byte[] decryptBase64StrInUtf8Bytes(RSA rsa, byte[] base64InUtf8Bytes) {
        return decryptBase64Str(rsa, new String(base64InUtf8Bytes, StandardCharsets.UTF_8));
    }

    public static String decryptBase64Str2Str(RSA rsa, String base64Str) {
        return new String(decryptBase64Str(rsa, base64Str), StandardCharsets.UTF_8);
    }
}