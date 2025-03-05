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
public class PluginController {
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
        return Optional.ofNullable(agentPowerApplicationService.importPlugin(file.getName(), file.getInputStream()))
                .orElse("导入成功");
    }

    @RequestMapping("uninstall")
    public String uninstallPlugin(@RequestParam String jarFileName) {
        return Optional.ofNullable(agentPowerApplicationService.uninstallPlugin(jarFileName))
                .orElse("卸载成功");
    }

    @RequestMapping("restore")
    public String restorePlugin(@RequestParam String jarFileName) {
        return Optional.ofNullable(agentPowerApplicationService.restorePlugin(jarFileName))
                .orElse("还原成功");
    }


}
