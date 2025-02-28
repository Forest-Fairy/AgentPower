package org.agentpower.configuration.resource.provider.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONPath;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.StreamResourceProvider;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;

public class LocalFileResourceProvider extends StreamResourceProvider {
    public static final String TYPE = "local-file";
    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ByteArrayResource getSource(ResourceProviderConfiguration resourceProviderConfiguration, String configId) {
        String filepath = (String) JSONPath.eval(
                resourceProviderConfiguration.getParams(), "$.filePath");
        return new ByteArrayResource(FileUtil.readBytes(filepath));
    }
}
