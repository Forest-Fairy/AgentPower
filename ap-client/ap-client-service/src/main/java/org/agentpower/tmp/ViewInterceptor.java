//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.agentpower.infrastructure;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idocv.docview.common.DocResponse;
import com.idocv.docview.exception.DocServiceException;
import com.idocv.docview.service.DocService;
import com.idocv.docview.service.SessionService;
import com.idocv.docview.service.UserService;
import com.idocv.docview.util.ConfUtil;
import com.idocv.docview.util.DateTimeUtil;
import com.idocv.docview.util.IpUtil;
import com.idocv.docview.util.RemoteUtil;
import com.idocv.docview.vo.DocVo;
import com.idocv.docview.vo.SessionVo;
import com.idocv.docview.vo.UserVo;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class ViewInterceptor extends HandlerInterceptorAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ViewInterceptor.class);
    @Resource
    private DocService docService;
    @Resource
    private UserService userService;
    @Resource
    private SessionService sessionService;
    private boolean thdViewCheckSwitch = false;
    private String thdViewCheckUri;
    private String thdViewCheckUrl;
    public static Map<String, String> thdViewCheckMap;
    private int viewPagePrivateSessionDuraion;
    private boolean thdViewCheckEverytime;
    private static ObjectMapper om = new ObjectMapper();

    public ViewInterceptor() {
    }

    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String contextPath = ConfInterceptor.getContextPath(req);
        String requestUri = req.getRequestURI();
        if (requestUri.matches("/.+/favicon.ico")) {
            resp.sendRedirect(contextPath + "/favicon.ico");
            return false;
        } else {
            String requestUrl;
            if (requestUri.startsWith(contextPath + "/view/")) {
                requestUrl = IpUtil.getIpAddr(req);
                if (requestUri.matches(contextPath + "/view/[^./]+")) {
                    logger.info("[REQUEST][" + requestUrl + "]View URL: " + getFullURL(req));
                } else if (requestUri.endsWith(".json")) {
                    logger.debug("[REQUEST][" + requestUrl + "]View URL: " + getFullURL(req));
                }
            }

            if (requestUri.startsWith(contextPath + "/doc/download/")) {
                requestUrl = IpUtil.getIpAddr(req);
                logger.info("[REQUEST][" + requestUrl + "]Down URL: " + getFullURL(req));
            }

            if (requestUri.startsWith(contextPath + "/doc/delete/")) {
                requestUrl = IpUtil.getIpAddr(req);
                logger.info("[REQUEST][" + requestUrl + "]Delete URL: " + getFullURL(req));
            }

            String downloadAuth;
            if (requestUri.startsWith(contextPath + "/view/") && requestUri.matches(contextPath + "/view/\\w{3,31}.json")) {
                requestUrl = req.getRequestURL().toString();
                if (requestUrl.contains("idocv.com")) {
                    downloadAuth = requestUri.replaceFirst(contextPath + "/view/(\\w{3,31}).json", "$1");
                    if (StringUtils.isNotBlank(downloadAuth) && downloadAuth.matches("\\w{24}")) {
                        downloadAuth = this.getUuidBySessionId(downloadAuth);
                    }

                    DocVo docVo = null;
                    if ("url".equals(downloadAuth)) {
                        String url = req.getParameter("url");
                        docVo = this.docService.getUrl(url, false);
                    } else {
                        docVo = this.docService.getByUuid(downloadAuth, false);
                    }

                    if (null == docVo) {
                        return true;
                    }

                    int viewCount = docVo.getViewCount();
                    String srcUrl = docVo.getUrl();
                    String docVoCtimeString = docVo.getCtime();
                    long docVoCtime = LocalDateTime.parse(docVoCtimeString, DateTimeUtil.dateTimeFormatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - docVoCtime > 3600000L && (null == srcUrl || !srcUrl.contains("idocv.com") && !srcUrl.startsWith("file://")) && !this.isAdminLogin(req)) {
                        Map<String, Object> respMap = DocResponse.getErrorResponseMap("为避免部分用户传播非法内容，测试链接限制1小时内有效，正式版用户无此限制");
                        resp.getWriter().write(JSON.toJSONString(respMap));
                        return false;
                    }
                }
            }

            if (this.thdViewCheckSwitch && this.isCheckUri(req)) {
                Map authMap;
                if (requestUri.startsWith(contextPath + "/doc/upload")) {
                    authMap = this.getRemoteAuthMap(req, (String)null);
                    setSessionAttribute(req, "authMap", authMap);
                    downloadAuth = (String)authMap.get("upload");
                    if ("0".equals(downloadAuth)) {
                        resp.setCharacterEncoding("UTF-8");
                        resp.setHeader("Content-type", "text/html;charset=UTF-8");
                        Map<String, Object> respMap = DocResponse.getErrorResponseMap("没有上传权限");
                        resp.getWriter().write(JSON.toJSONString(respMap));
                        return false;
                    }
                }

                if (requestUri.startsWith(contextPath + "/doc/download")) {
                    authMap = this.getRemoteAuthMap(req, (String)null);
                    setSessionAttribute(req, "authMap", authMap);
                    downloadAuth = (String)authMap.get("down");
                    if ("0".equals(downloadAuth)) {
                        resp.setCharacterEncoding("UTF-8");
                        resp.setHeader("Content-type", "text/html;charset=UTF-8");
                        resp.getWriter().write("没有下载权限");
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public void postHandle(HttpServletRequest req, HttpServletResponse resp, Object handler, ModelAndView model) throws Exception {
        String contextPath = ConfInterceptor.getContextPath(req);
        String requestUri = req.getRequestURI();
        if (this.thdViewCheckSwitch && this.isCheckUri(req)) {
            String uriRegex = contextPath + "/view/(\\w{3,31})";
            if (requestUri.startsWith(contextPath + "/view/") && requestUri.matches(uriRegex)) {
                String reqUrl = getFullURL(req);
                String reqUrlMd5 = DigestUtils.md5Hex(reqUrl);
                setSessionAttribute(req, "reqUrl", reqUrl);
                setSessionAttribute(req, "reqUrlMd5", reqUrlMd5);
                String checkAuthValue;
                if (this.isChecked(req, reqUrlMd5)) {
                    String checkAuthKey = "IDOCV_CHK_AUTH_" + reqUrlMd5;
                    checkAuthValue = (String)getSessionAttribute(req, checkAuthKey);
                    if (StringUtils.isNoneBlank(new CharSequence[]{checkAuthValue})) {
                        checkAuthValue = URLDecoder.decode(checkAuthValue, "UTF-8");
                        if ("0".equals(JSON.parseObject(checkAuthValue).get("view"))) {
                            throw new Exception("没有预览权限");
                        }
                    }

                    return;
                }

                Map<String, String> authMap = this.getRemoteAuthMap(req, (String)null);
                checkAuthValue = "IDOCV_CHK_AUTH_" + reqUrlMd5;
                setSessionAttribute(req, checkAuthValue, JSON.toJSONString(authMap));
                String viewAuth = (String)authMap.get("view");
                if ("0".equals(viewAuth)) {
                    throw new Exception("没有预览权限");
                }
            }
        }

    }

    public static String clearXss(String value) {
        if (value != null && !"".equals(value)) {
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            value = value.replaceAll("\"", "%22");
            return value;
        } else {
            return value;
        }
    }

    public static String getCookie(HttpServletRequest req, String key) {
        Cookie[] cookies = req.getCookies();
        if (null != cookies && cookies.length > 0) {
            Cookie[] var3 = cookies;
            int var4 = cookies.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Cookie cookie = var3[var5];
                if (key.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return "";
    }

    public boolean isChecked(HttpServletRequest req, String reqUrlMd5) {
        try {
            if (this.thdViewCheckEverytime) {
                return false;
            }

            String cookieAuthKey = "IDOCV_CHK_AUTH_" + reqUrlMd5;
            String cookieAuthValue = (String)getSessionAttribute(req, cookieAuthKey);
            if (StringUtils.isNotBlank(cookieAuthValue)) {
                logger.debug("上次文档权限验证有效: " + cookieAuthKey + "=" + cookieAuthValue);
                return true;
            }

            logger.debug("上次文档权限验证已过期，需要重新验证cookieAuthKey: " + cookieAuthKey);
        } catch (Exception var5) {
            Exception e = var5;
            logger.info("验证用户上一次预览权限失败：" + e.getMessage());
        }

        return false;
    }

    public String getUuidBySessionId(String sessionId) throws DocServiceException {
        if (!StringUtils.isBlank(sessionId) && sessionId.matches("\\w{24}")) {
            SessionVo sessionVo = this.sessionService.get(sessionId);
            if (null == sessionVo) {
                logger.warn("[ERROR] getUuidBySessionId 无效的会话ID.");
                throw new DocServiceException("无效的会话ID！");
            } else {
                String uuid = sessionVo.getUuid();
                return uuid;
            }
        } else {
            return "";
        }
    }

    public Map<String, String> getRemoteAuthMap(HttpServletRequest req, String uuid) {
        String requestUri = req.getRequestURI();
        Map<String, String> authMap = new HashMap(thdViewCheckMap);
        if (StringUtils.isNotBlank(this.thdViewCheckUrl)) {
            String sessionId = req.getSession().getId();
            String checkUrl = this.thdViewCheckUrl + (this.thdViewCheckUrl.contains("?") ? "&" : "?") + "uri=" + requestUri + "&sessionid=" + sessionId;
            if (StringUtils.isNoneBlank(new CharSequence[]{uuid})) {
                checkUrl = checkUrl + "&uuid=" + uuid;
            }

            String queryString = req.getQueryString();
            if (StringUtils.isNoneBlank(new CharSequence[]{queryString})) {
                checkUrl = checkUrl + "&" + queryString;
            }

            try {
                String str = RemoteUtil.getByJsoup(req, checkUrl);
                Map<String, String> remoteMap = (Map)om.readValue(str, new TypeReference<HashMap<String, String>>() {
                });
                authMap.putAll(remoteMap);
            } catch (Exception var10) {
                Exception e = var10;
                logger.warn("[REMOTE GET] URL(" + checkUrl + "), EXCEPTION(" + e.getMessage() + ")");
            }
        }

        if (null != req.getParameter("puuid")) {
            authMap.put("view", "1");
        }

        return authMap;
    }

    private boolean isAdminLogin(HttpServletRequest req) {
        boolean isAdmin = false;

        try {
            String sid = getCookie(req, "IDOCVSID");
            if (StringUtils.isNotBlank(sid)) {
                UserVo vo = this.userService.getBySid(sid);
                if (null != vo && 100 == vo.getStatus()) {
                    logger.debug("验证管理员登录：是管理员");
                    isAdmin = true;
                }
            }
        } catch (Exception var5) {
            Exception e = var5;
            logger.debug("判断管理员登录失败: " + e.getMessage());
        }

        return isAdmin;
    }

    private boolean isCheckUri(HttpServletRequest req) {
        String reqUri = req.getRequestURI();
        if (!StringUtils.isBlank(reqUri) && !StringUtils.isBlank(this.thdViewCheckUri)) {
            String[] checkUris = this.thdViewCheckUri.split(",|;");
            String[] var4 = checkUris;
            int var5 = checkUris.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String checkUri = var4[var6];
                if (reqUri.contains(checkUri) || reqUri.matches(checkUri)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static String getFullURL(HttpServletRequest req) {
        StringBuilder requestURL = new StringBuilder(req.getRequestURL().toString());
        String queryString = req.getQueryString();
        return queryString == null ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
    }

    public static void setSessionAttribute(HttpServletRequest req, String key, Object value) {
        req.getSession().setAttribute(key, value);
    }

    public static Object getSessionAttribute(HttpServletRequest req, String key) {
        return req.getSession().getAttribute(key);
    }

    static {
        String thdViewCheckDefault = ConfUtil.getProperty("thd.view.check.default");
        thdViewCheckMap = (Map)JSON.parseObject(thdViewCheckDefault, new com.alibaba.fastjson2.TypeReference<Map<String, String>>() {
        }, new JSONReader.Feature[0]);
    }
}
