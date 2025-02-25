package org.agentpower.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.util.Map;

@Controller
@RequestMapping("client")
public class AgentPowerClient implements org.agentpower.api.AgentPowerClient {
    public record FileContentObj(
            String fileAbsPath,
            boolean existOrNot,
            byte[] fileContentIfExist) { }

    @Override
    @PostMapping("getFileContent")
    public Map<String, Object> getFileContent(String fileAbsPath) {
        File file = new File(fileAbsPath);
        return JSONUtil.parseObj(new FileContentObj(
                fileAbsPath,
                file.exists(),
                file.exists() ? FileUtil.readBytes(file) : null));
    }

}
