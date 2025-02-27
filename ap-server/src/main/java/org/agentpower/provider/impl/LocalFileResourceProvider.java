package org.agentpower.provider.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONPath;
import org.agentpower.configuration.provider.ResourceProviderConfiguration;
import org.agentpower.provider.StreamResourceProvider;

import java.io.InputStream;

public class LocalFileResourceProvider extends StreamResourceProvider {
    public static final String TYPE = "local-file";
    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public InputStream getSource(ResourceProviderConfiguration resourceProviderConfiguration, String configId) {
        String filepath = (String) JSONPath.eval(
                resourceProviderConfiguration.getParams(), "$.filePath");
        return FileUtil.getInputStream(filepath);
    }
}
