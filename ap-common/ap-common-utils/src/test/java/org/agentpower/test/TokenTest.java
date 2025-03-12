package org.agentpower.test;

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
        KeyPair keyPair = RSAUtil.generateKeyPair(RSAUtil.ALGORITHM);
        String encrypt = RSAUtil.encrypt(jwt, keyPair.getPublic());
        System.out.println(encrypt);
        String decrypt = RSAUtil.decrypt(encrypt, keyPair.getPrivate());
        System.out.println(decrypt);
        JWT parsedJwt = JwtUtil.parseJWT(decrypt, "test123".getBytes(StandardCharsets.UTF_8));
        System.out.println(parsedJwt.validate(0));
        Thread.sleep(12000L);
        System.out.println(parsedJwt.validate(0));

    }
}
