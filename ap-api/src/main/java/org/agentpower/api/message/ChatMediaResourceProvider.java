package org.agentpower.api.message;

import java.util.List;

/**
 * 资源提供者
 * @param configId 资源提供者配置id 空表示不需要提供者 直接使用资源内容
 * @param mediaList 多媒体资源列表
 */
public record ChatMediaResourceProvider(
        String configId,
        List<ChatMediaResource> mediaList) {
}