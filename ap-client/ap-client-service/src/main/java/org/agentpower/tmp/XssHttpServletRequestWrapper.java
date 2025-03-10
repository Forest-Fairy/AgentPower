//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.agentpower.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public String getHeader(String name) {
        return ViewInterceptor.clearXss(super.getHeader(name));
    }

    public String getQueryString() {
        return super.getQueryString();
    }

    public String getParameter(String name) {
        return ViewInterceptor.clearXss(super.getParameter(name));
    }

    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return super.getParameterValues(name);
        } else {
            int length = values.length;
            String[] escapseValues = new String[length];

            for(int i = 0; i < length; ++i) {
                escapseValues[i] = ViewInterceptor.clearXss(values[i]);
            }

            return escapseValues;
        }
    }
}
