//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.agentpower.infrastructure;

import com.idocv.docview.common.XssHttpServletRequestWrapper;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class XssFilter implements Filter {
    public XssFilter() {
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String requestUri = ((HttpServletRequest)req).getRequestURI();
        String contextPath = ConfInterceptor.getContextPath((HttpServletRequest)req);
        if (requestUri.startsWith(contextPath + "/edit/") && requestUri.endsWith("/save")) {
            chain.doFilter(req, resp);
        } else {
            chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest)req), resp);
        }
    }

    public void destroy() {
    }
}
