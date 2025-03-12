package org.agentpower.common;

import cn.hutool.jwt.JWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    /**
     * 创建JWT
     * @param jwtId             JWT的ID
     * @param subject           JWT主体信息，一般以Json格式的数据存储
     * @param issuer            JWT签发者
     * @param expiredDuration   JWT的有效时间，单位是毫秒
     * @param claims            创建payload的私有声明，也就是自定义的JWT信息 可为空
     * @param secretKey         秘钥
     * @return jwt token
     */
    public static String createJWT(String jwtId, String subject, String issuer, long expiredDuration,
                                   Map<String, Object> claims, byte[] secretKey) {
        return JWT.create()
                .setJWTId(jwtId)
                .setIssuedAt(new Date())
                .setIssuer(issuer)
                .setExpiresAt(new Date(System.currentTimeMillis() + expiredDuration))
                .setSubject(subject)
                .addPayloads(claims)
                .setKey(secretKey)
                .sign();
    }

    /**
     * 解密JWT，如果秘钥错误，或者JWT被修改过，会直接抛出异常信息
     *
     * @param jwt           JWT字符串信息
     * @param secretKey     秘钥
     * @return 返回JWT载荷信息
     */
    public static JWT parseJWT(String jwt, byte[] secretKey) {
        return JWT.of(jwt).setKey(secretKey);
    }

    /**
     * 判断jwt是否已经过期
     *
     * @param jwt JWT字符串信息
     * @return 如果jwt已经过期，返回true，否则返回false
     */
    public static boolean isExpiration(String jwt) {
        return JWT.of(jwt).validate(0);
    }
}
