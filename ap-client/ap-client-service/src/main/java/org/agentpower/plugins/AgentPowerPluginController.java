package org.agentpower.plugins;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("plugins")
@AllArgsConstructor
public class AgentPowerPluginController {
    private final AgentPowerPluginService pluginService;
    @RequestMapping("list/installed")
    public List<AgentPowerPluginVo> listInstalled() {
        return pluginService.listInstalledPlugins();
    }

    @RequestMapping("list/uninstalled")
    public List<AgentPowerPluginVo> listUninstalled() {
        return pluginService.listUninstalledPlugins();
    }

    @RequestMapping("import")
    public String importPlugin(@RequestPart MultipartFile file) throws IOException {
        return pluginService.importPlugin(file.getName(), file.getInputStream());
    }

    @RequestMapping("uninstall")
    public String uninstallPlugin(@RequestParam String jarFileName) {
        return pluginService.uninstallPlugin(jarFileName);
    }

    @RequestMapping("restore")
    public String restorePlugin(@RequestParam String jarFileName) {
        return pluginService.restorePlugin(jarFileName);
    }

    @RequestMapping("startPluginPathWatch")
    public void startPluginWatch() {
        pluginService.startWatch();
    }
    @RequestMapping("stopPluginPathWatch")
    public void stopPluginWatch() {
        pluginService.stopWatch();
    }
    @RequestMapping("clearFailedCache")
    public int clearFailedCache() {
        return pluginService.clearFailedCache();
    }
    @RequestMapping("clearUninstalledCache")
    public int clearUninstalledCache() {
        return pluginService.clearUninstalledCache();
    }

}
