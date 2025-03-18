package org.agentpower.service.secure.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface Encoder {
    /**
     * 编码
     * @param data 如果数据是String，那将被UTF8 转成bytes
     * @return byte[]
     */
    byte[] encode(byte[] data);

    default String encodeToBase64Str(String data) {
        return Base64.getEncoder().encodeToString(
                this.encode(data.getBytes(StandardCharsets.UTF_8)));
    }
}
