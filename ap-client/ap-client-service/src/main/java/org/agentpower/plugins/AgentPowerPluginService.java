package org.agentpower.plugins;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.agentpower.client.service.AgentPowerClientServiceImpl;
import org.agentpower.infrastructure.AgentPowerFunction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
@Component
@EnableConfigurationProperties({AgentPowerPluginProperties.class})
public class AgentPowerPluginService implements ApplicationListener<ApplicationContextInitializedEvent> {

    private int detectDelay;
    private volatile ScheduledExecutorService watchService;
    private AgentPowerPluginInstallationService installationService;

    public AgentPowerPluginService() {
    }

    /**
     * 列出已安装的插件
     * @return 插件包
     */
    public List<AgentPowerPluginVo> listInstalledPlugins() {
        return installationService.listPlugins();
    }

    /**
     * 列出已卸载的插件
     * @return 插件包
     */
    public List<AgentPowerPluginVo> listUninstalledPlugins() {
        return installationService.listUninstalledPlugins();
    }

    /**
     * 导入插件
     * @param jarFileName 插件包名
     * @param inputStream 插件包资源
     * @return 错误信息
     */
    public String importPlugin(String jarFileName, InputStream inputStream) {
        if (jarFileName.replace("\\", "/").indexOf("/") > 0) {
            return "操作失败，插件包名不能包含路径";
        } else if (! jarFileName.toLowerCase().endsWith("\\.jar")) {
            return "操作失败，插件只支持jar包";
        }
        return installationService.importPlugin(jarFileName, inputStream);
    }

    /**
     * 卸载插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String uninstallPlugin(String jarFileName) {
        if (jarFileName.replace("\\", "/").indexOf("/") > 0) {
            return "操作失败，插件包名不能包含路径";
        }
        return installationService.uninstallPlugin(jarFileName);
    }

    /**
     * 还原插件
     * @param jarFileName 插件包名
     * @return 错误信息
     */
    public String restorePlugin(String jarFileName) {
        if (jarFileName.replace("\\", "/").indexOf("/") > 0) {
            return "操作失败，插件包名不能包含路径";
        }
        return installationService.restorePlugin(jarFileName);
    }

    /**
     * 清除失败插件包
     * @return 删除数量
     */
    public int clearFailedCache() {
        return installationService.clearFailedCache();
    }

    /**
     * 清除已卸载插件包
     * @return 删除数量
     */
    public int clearUninstalledCache() {
        return installationService.clearUninstalledCache();
    }

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        val applicationContext = event.getApplicationContext();
        val clientService = applicationContext.getBean(AgentPowerClientServiceImpl.class);
        val properties = applicationContext.getBean(AgentPowerPluginProperties.class);
        buildingInstallationService(applicationContext, clientService, properties);
        startWatch();
    }

    private void buildingInstallationService(ConfigurableApplicationContext applicationContext, AgentPowerClientServiceImpl clientService, AgentPowerPluginProperties properties) {
        Objects.requireNonNull(properties);
        detectDelay = properties.getDetectDelay() == null ? 5 : properties.getDetectDelay();
        File pluginPath = StringUtils.isBlank(properties.getPluginPath())
                ? new File("./plugins")
                : new File(properties.getPluginPath());
        File pluginDetectPath = StringUtils.isBlank(properties.getPluginDetectPath())
                ? new File(pluginPath, "detect")
                : new File(properties.getPluginDetectPath());
        File pluginRecyclePath = StringUtils.isBlank(properties.getPluginRecyclePath())
                ? new File(pluginPath, "uninstalled")
                : new File(properties.getPluginRecyclePath());
        File pluginFailedPath = StringUtils.isBlank(properties.getPluginFailedPath())
                ? new File(pluginPath, "failed")
                : new File(properties.getPluginFailedPath());
        File pluginImportPath = StringUtils.isBlank(properties.getPluginImportPath())
                ? new File(pluginPath, "importing")
                : new File(properties.getPluginImportPath());
        this.installationService = new AgentPowerPluginInstallationService(
                applicationContext, clientService,
                pluginPath, pluginDetectPath, pluginRecyclePath, pluginFailedPath, pluginImportPath);
    }


    /**
     * 启动监听
     */
    public synchronized void startWatch() {
        if (this.watchService == null || this.watchService.isShutdown()) {
            watchService = Executors.newSingleThreadScheduledExecutor();
            log.info("插件监听器已启动");
            watchService.scheduleAtFixedRate(() -> {
                try {
                    installationService.detectAndUpdateIfNeeded();
                } catch (InterruptedException e) {
                    // 线程中断时关闭调度器
                    watchService.shutdown();
                }
            }, 0, this.detectDelay, TimeUnit.SECONDS);  // 延迟0秒后每n秒执行一次
        }
    }

    /**
     * 停止监听
     */
    public synchronized void stopWatch() {
        if (this.watchService != null && !this.watchService.isShutdown()) {
            this.watchService.shutdownNow();
            log.info("插件监听器已停止");
        }
    }

}