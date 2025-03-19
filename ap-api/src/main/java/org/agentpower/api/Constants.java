package org.agentpower.api;

public class Constants {
    private Constants() {}
    public static final String CONFIG_PREFIX = "agent-power";
    /** 默认认证字段的头部 */
    public static final String DEFAULT_RECOGNIZER_HEADER_FIELD = "token";

    /** 定义头参数字段 对应 {@link FunctionRequest#getHeader()} */
    public static final String PARAM_HEADER = "header";
    /** 定义请求体参数字段 对应 {@link FunctionRequest#getBody()} */
    public static final String PARAM_BODY = "body";


    public static class Header {
        public static final String ENCODED_KEY = "enc-key";
        public static final String ALGORITHM = "enc-algorithm";
    }
    public static class Body {
        public static final String CLIENT_CONFIGURATION_ID = "configurationId";
        public static final String SERVICE_URL = "serviceUrl";
        public static final String TOOL_NAME = "toolName";
        public static final String TOOL_PARAMS = "toolParams";
    }

    public static class Event {
        public static final String FUNC_CALL = "func-call";
        public static final String AGENT_CALL = "agent-call";
        public static final String GET_FUNCTION = "get-function";
        public static final String LIST_FUNCTIONS = "list-functions";

        /** 前端提示失败 */
        public static final String ALERT_FAILED = "alert-failed";
        /** 前端提示失败 */
        public static final String ALERT_WARNING = "alert-warning";
    }
}
