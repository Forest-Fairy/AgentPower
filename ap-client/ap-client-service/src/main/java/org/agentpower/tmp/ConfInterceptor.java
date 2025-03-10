package org.agentpower.infrastructure;

import com.idocv.docview.common.ViewType;
import com.idocv.docview.util.ConfUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ConfInterceptor implements HandlerInterceptor {
    private static String version = ConfUtil.getProperty("version");
    private static String env = ConfUtil.getProperty("env");
    private static String dataUrl = ConfUtil.getProperty("url");
    private static String filetypeView;
    private static String confDrawServer;

    public ConfInterceptor() {
    }

    public void postHandle(HttpServletRequest req, HttpServletResponse response, Object handler, ModelAndView model) throws Exception {
        if (null != model) {
            setBasicParams(req);
        }

    }

    public static String getContextPath(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        if (StringUtils.isBlank(contextPath) && dataUrl.endsWith("/data/")) {
            contextPath = dataUrl.replaceFirst("(https?://[^/]+)?(/[^/]+)?/data/", "$2");
        }

        return contextPath;
    }

    public static void setBasicParams(HttpServletRequest req) {
        if (null != req) {
            req.setAttribute("version", version);
            req.setAttribute("env", env);
            req.setAttribute("filetypeView", filetypeView);
            req.setAttribute("confDrawServer", confDrawServer);
            req.setAttribute("contextPath", getContextPath(req));
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        ConfInterceptor.version = version;
    }

    static {
        filetypeView = ViewType.viewTypes;
        confDrawServer = ConfUtil.getProperty("view.page.draw.server");
    }
}
