package org.agentpower.configuration.resource.provider.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONPath;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.ByteArrayResourceProvider;
import org.springframework.core.io.ByteArrayResource;

public class LocalFileResourceProvider extends ByteArrayResourceProvider {
    public static final String TYPE = "local-file";
    @Override
    public String type() {
        return TYPE;
    }

    @Override
    protected byte[] getByteArraySource(ResourceProviderConfiguration resourceProviderConfiguration, String configId) {
        String filepath = (String) JSONPath.eval(
                resourceProviderConfiguration.getParams(), "$.filePath");
        return FileUtil.readBytes(filepath);
    }

}
