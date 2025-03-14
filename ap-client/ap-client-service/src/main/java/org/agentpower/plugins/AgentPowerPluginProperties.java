package org.agentpower.plugins;

import lombok.Getter;
import org.agentpower.api.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(Constants.CONFIG_PREFIX + "." + "plugins")
public class AgentPowerPluginProperties {
    /** 检测间隔 单位秒 默认 5 */
    private Integer detectDelay;
    /** 已安装的插件目录 */
    private String pluginPath;
    /** 自动侦测的插件目录 */
    private String pluginDetectPath;
    /** 插件回收站目录 */
    private String pluginRecyclePath;
    /** 安装失败路径 */
    private String pluginFailedPath;
    /** 导入路径 */
    private String pluginImportPath;
}
