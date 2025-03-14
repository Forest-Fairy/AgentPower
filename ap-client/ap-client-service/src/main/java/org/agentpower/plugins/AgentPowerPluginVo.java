package org.agentpower.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class AgentPowerPluginVo {
    private String jarFileName;
    private String pluginStatus;
    private int pluginBeanCount;
    private long pluginFileSize;
    private Set<String> pluginFunctions;
}
