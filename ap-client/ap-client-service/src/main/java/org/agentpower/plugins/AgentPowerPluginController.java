package org.agentpower.plugins;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("plugins")
@AllArgsConstructor
public class AgentPowerPluginController {
    private final AgentPowerApplicationService agentPowerApplicationService;
    @RequestMapping("list/installed")
    public List<String> listInstalled() {
        return agentPowerApplicationService.listInstalledPlugins();
    }

    @RequestMapping("list/uninstalled")
    public List<String> listUninstalled() {
        return agentPowerApplicationService.listUninstalledPlugins();
    }

    @RequestMapping("import")
    public String importPlugin(@RequestPart MultipartFile file) throws IOException {
        return agentPowerApplicationService.importPlugin(file.getName(), file.getInputStream());
    }

    @RequestMapping("uninstall")
    public String uninstallPlugin(@RequestParam String jarFileName) {
        return agentPowerApplicationService.uninstallPlugin(jarFileName);
    }

    @RequestMapping("restore")
    public String restorePlugin(@RequestParam String jarFileName) {
        return agentPowerApplicationService.restorePlugin(jarFileName);
    }


}
