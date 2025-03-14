package org.agentpower.test;

import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson2.JSON;
import org.agentpower.common.JwtUtil;
import org.agentpower.common.RSAUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;

public class TokenTest {
    @Test
    public void generateToken() throws InterruptedException {
        String jwt = JwtUtil.createJWT("temID",
                JSON.toJSONString(Map.of(
                        "name", "hello",
                        "id", "hey"
                )),
                "agentpower", 1000L * 10,
                Map.of(
                        "body1", "body1"
                ),
                "test123".getBytes(StandardCharsets.UTF_8));
        // JWT 只是验签 不会对数据体加密 因此可以
        // 1 用JWT 秘钥生成签名
        // 2 用RSA 公钥加密
        // 3 用RSA 私钥解密
        // 4 用JWT 秘钥进行验签
//        SignAlgorithm[] enumConstants = RSAUtil.algorithmClass.getEnumConstants();
        KeyPair keyPair = RSAUtil.generateKeyPair(SignAlgorithm.SHA512withRSA.getValue());
        RSA rsa = RSAUtil.create(keyPair.getPublic(), keyPair.getPrivate());
        String encrypt = RSAUtil.encryptUtf8Str2Base64Str(rsa, jwt);
        System.out.println(encrypt);
        String decrypt = RSAUtil.decryptBase64Str2Str(rsa, encrypt);
        System.out.println(decrypt);
        JWT parsedJwt = JwtUtil.parseJWT(decrypt);
        System.out.println(parsedJwt.validate(0));
        System.out.println(JwtUtil.isValid(parsedJwt, "test123".getBytes(StandardCharsets.UTF_8)));
        Thread.sleep(12000L);
        System.out.println(parsedJwt.validate(0));

    }
}
