package org.agentpower.agent.dto;

import java.util.List;

/**
 * 资源提供者
 * @param configId              资源提供者配置id
 * @param mediaList             多媒体资源列表
 */
public record ChatMediaResourceProvider(
        String configId,
        List<ChatMediaResource> mediaList) {

}