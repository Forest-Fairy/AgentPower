package org.agentpower.server;

import org.agentpower.agent.func.FileReadFunc;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping(org.agentpower.api.AgentPowerServer.Const.SERVER)
public class AgentPowerServer {

    @RequestMapping("receiveFileContent")
    public int receiveFileContent(String requestId, String fileAbsPath, MultipartFile fileContentIfExist) throws IOException {
        return FileReadFunc.receiveFileContent(requestId, fileAbsPath, fileContentIfExist.getInputStream());
    }
}
