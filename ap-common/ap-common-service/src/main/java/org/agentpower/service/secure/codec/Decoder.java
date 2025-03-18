package org.agentpower.service.secure.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface Decoder {
    /**
     * 解码
     * @param decoded 如果密文是String，那将被Base64转成bytes
     * @return byte[]
     */
    byte[] decode(byte[] decoded);

    default String decodeToUtf8Str(String base64Str) {
        return new String(this.decode(Base64.getDecoder().decode(base64Str)), StandardCharsets.UTF_8);
    }
}
