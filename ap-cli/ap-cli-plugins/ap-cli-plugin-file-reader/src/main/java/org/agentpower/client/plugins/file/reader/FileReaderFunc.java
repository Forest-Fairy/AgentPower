package org.agentpower.client.plugins.file.reader;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.client.func.AutoResolveFunction;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("fileReader")
@Description("读取客户端文件内容")
public class FileReaderFunc extends AutoResolveFunction implements Function<FileReaderFunc.Request, String> {
    @Override
    public String apply(Request request) {
        return FileUtil.readString(request.filePath, "");
    }

    public record Request(
            @JsonProperty(required = true) @JsonPropertyDescription(value = "filePath") String filePath,
            @JsonProperty(defaultValue = "utf-8") @JsonPropertyDescription(value = "encoding") String encoding) {}
}
