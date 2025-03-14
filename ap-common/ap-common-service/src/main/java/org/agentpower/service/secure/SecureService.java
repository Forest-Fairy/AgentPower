package org.agentpower.service.secure;

import jakarta.servlet.Filter;
import org.springframework.web.servlet.HandlerInterceptor;

public interface SecureService extends HandlerInterceptor, Filter {
}
