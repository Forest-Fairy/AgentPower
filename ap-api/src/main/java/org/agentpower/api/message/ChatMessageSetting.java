package org.agentpower.api.message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @param clientAgentModelId    客户端代理模型配置id -> 不可为空
 * @param clientAgentServiceId  客户端代理增强配置id -> 空表示无启用
 * @param knowledgeBaseId       知识库id           -> 空表示不启用向量检索
 * @param resourceProviders     资源提供者列表
 */
public record ChatMessageSetting(
        String clientAgentModelId,
        String clientAgentServiceId,
        String knowledgeBaseId,
        List<ChatMediaResourceProvider> resourceProviders) {
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private boolean enableVectorStore;
        private String clientAgentModelId;
        private String clientAgentServiceId;
        private String knowledgeBaseId;
        private List<ChatMediaResourceProvider> resourceProviders;

        public Builder() {
            this.enableVectorStore = false;
            this.clientAgentModelId = "";
            this.clientAgentServiceId = "";
            this.resourceProviders = new LinkedList<>();
        }
        public Builder enableVectorStore(boolean enableVectorStore) {
            this.enableVectorStore = enableVectorStore;
            return this;
        }
        public Builder clientAgentModelId(String clientAgentModelId) {
            this.clientAgentModelId = clientAgentModelId;
            return this;
        }
        public Builder clientAgentServiceId(String clientAgentServiceId) {
            this.clientAgentServiceId = clientAgentServiceId;
            return this;
        }
        public Builder knowledgeBaseId(String knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
            return this;
        }
        public Builder addResourceProvider(ChatMediaResourceProvider... resourceProviders) {
            this.resourceProviders.addAll(List.of(resourceProviders));
            return this;
        }
        public Builder addDirectResource(String mediaType, byte[] mediaContent) {
            this.resourceProviders.add(new ChatMediaResourceProvider(null, List.of(new ChatMediaResource(null, mediaType, mediaContent))));
            return this;
        }
        public Builder addReferenceResource(String providerConfigId, String mediaId) {
            this.resourceProviders.add(new ChatMediaResourceProvider(providerConfigId, List.of(new ChatMediaResource(mediaId, null, null))));
            return this;
        }
        public ChatMessageSetting build() {
            List<ChatMediaResourceProvider> providers = this.resourceProviders.stream().collect(Collectors.groupingBy(
                    provider -> provider.configId() == null || provider.configId().isBlank() ? "" : provider.configId(),
                    HashMap::new,
                    Collectors.toList()
            )).entrySet().stream().map(entry ->
                    new ChatMediaResourceProvider(entry.getKey(),
                            entry.getValue().stream().map(ChatMediaResourceProvider::mediaList).flatMap(List::stream).toList())
            ).toList();
            return new ChatMessageSetting(enableVectorStore, clientAgentModelId, clientAgentServiceId, knowledgeBaseId, providers);
        }
    }
}