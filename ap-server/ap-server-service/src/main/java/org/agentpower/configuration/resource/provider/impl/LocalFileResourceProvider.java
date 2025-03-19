package org.agentpower.configuration.resource.provider.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONPath;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.provider.ResourceProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

public class LocalFileResourceProvider extends ResourceProvider {
    public static final String TYPE = "local-file";
    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String desc() {
        return "加载本地文件";
    }

    @Override
    public Media getSource(ResourceProviderConfiguration resourceProviderConfiguration, String resourceId) {
        String filepath = resourceId;
        return new Media(parseMimeTypeWithFileName(filepath), new ByteArrayResource(FileUtil.readBytes(filepath)));
    }

    public static MimeType parseMimeTypeWithFileName(String fileName) {
        String type = FileTypeUtil.getType(fileName);
        return MimeType.valueOf(type == null ? "application/octet-stream" : type);
    }

}
