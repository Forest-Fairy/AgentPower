package org.agentpower.client.func;

import org.agentpower.api.AgentPowerFunction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.Optional;

public abstract class AutoResolveFunction implements AgentPowerFunction {
    protected final String functionName;
    protected final String functionDesc;
    protected final String functionParamSchema;
    protected AutoResolveFunction() {
        String functionName = Optional.ofNullable(this.getClass().getAnnotation(Component.class))
                .map(Component::value)
                .filter(StringUtils::isNotBlank)
                // TODO 参考spring 如何获取函数名称
                .orElse("");
        String desc = Optional.ofNullable(this.getClass().getAnnotation(Description.class))
                .map(Description::value)
                .filter(StringUtils::isNotBlank)
                // TODO 参考spring 如何获取函数描述
                .orElse("");

        // TODO 参考spring 如何获取函数参数
        String functionParamSchema = null;
        this.functionName = functionName;
        this.functionDesc = desc;
        this.functionParamSchema = functionParamSchema;
    }

    @Override
    public String functionName() {
        return functionName;
    }

    @Override
    public String functionDesc() {
        return functionDesc;
    }

    @Override
    public String functionParamSchema() {
        return functionParamSchema;
    }
}
