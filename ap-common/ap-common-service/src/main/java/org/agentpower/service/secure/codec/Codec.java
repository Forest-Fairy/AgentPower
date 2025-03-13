package org.agentpower.service.secure.codec;

public interface Codec {
    /**
     * 编码
     * @param data 如果数据是String，那将被UTF8 转成bytes
     * @return byte[]
     */
    byte[] encode(byte[] data);

    /**
     * 解码
     * @param decoded 如果密文是String，那将被Base64转成bytes
     * @return byte[]
     */
    byte[] decode(byte[] decoded);
}
