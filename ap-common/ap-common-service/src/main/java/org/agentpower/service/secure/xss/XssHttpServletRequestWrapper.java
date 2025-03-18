package org.agentpower.service.secure.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public String getHeader(String name) {
        return castXss(super.getHeader(name));
    }

    public static String castXss(String value) {
        if (StringUtils.isNotBlank(value)) {
            return StringUtils.replaceEach(value,
                    new String[]{"<", ">", "\""},
                    new String[]{"&lt;", "&gt;", "%22"});
        } else {
            return value;
        }
    }
}
