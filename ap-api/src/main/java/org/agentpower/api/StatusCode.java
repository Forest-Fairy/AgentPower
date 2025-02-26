package org.agentpower.api;

public enum StatusCode {
    ;
    /* 请求成功 */
    public static final int OK = 0;
    /* 请求失败（未知域名等网络错误） */
    public static final int FAIL = -1;
    /* 请求超时（服务端接收响应超时） */
    public static final int REQUEST_TIME_OUT = -2;
    /* 请求终止（服务端接收到响应但是等待已超时） */
    public static final int REQUEST_ABORT = -3;
    /* 插件未安装 */
    public static final int PLUGIN_MISSING = -4;

}
