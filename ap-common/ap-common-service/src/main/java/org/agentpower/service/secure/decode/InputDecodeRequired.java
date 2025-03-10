package org.agentpower.service.secure.decode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义请求参数中需要解码的字段
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InputDecodeRequired {
    /**
     * @return 需要解码的字段
     */
    String[] fields();

    /**
     * @return 解码后的字段 -> 为空时保持一致，否则需要长度相同，一一对应，为空则保持默认
     */
    String[] projects() default {};

}
