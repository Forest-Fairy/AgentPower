package org.agentpower.agent.dto;

/**
 * 多媒体资源
 * @param id 资源id
 * @param mediaType 资源类型 -> 转换为 spring ai 的类型
 * @param mediaData 资源数据 -> 如一段音频或一段视频
 */
public record ChatMediaResource(
        String id,
        String mediaType,
        byte[] mediaData) {

}