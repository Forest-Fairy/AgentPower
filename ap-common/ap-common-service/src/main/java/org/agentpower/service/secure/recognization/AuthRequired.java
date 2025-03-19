package org.agentpower.service.secure.recognization;

import org.agentpower.common.SearchUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

public abstract class AuthRequired {
    private static final Map<String, AuthRequired> HANDLERS = new HashMap<>();
    protected AuthRequired(String... types) {
        for (String type : types) {
            HANDLERS.put(type, this);
        }
    }

    protected abstract void auth(String requiredType, LoginUserVo loginUserVo);

    public static void Auth(String requiredType, LoginUserVo loginUserVo) {
        Objects.requireNonNull(requiredType);
        AuthRequired handler = HANDLERS.get(requiredType);
        if (handler == null) {
            throw new IllegalArgumentException("未找到权限认证器");
        }
        handler.auth(requiredType, loginUserVo);
    }

    private static final AuthRequired Default = new AuthRequired(Types.values()) {

        void authLogin(LoginUserVo loginUser) {
            if (StringUtils.isBlank(loginUser.getId())) {
                throw PermissionInvalidException.invalidPermission("用户未登录");
            }
        }
        void authAdmin(LoginUserVo loginUser) {
            if (loginUser.getPrivileges() == null || ! SearchUtils.containsAnyTypes(
                    loginUser.getPrivileges(), ";", "ADMIN")) {
                throw PermissionInvalidException.invalidPermission("没有权限访问");
            }
        }
        void authManager(LoginUserVo loginUser) {
            if (loginUser.getPrivileges() == null || ! SearchUtils.containsAnyTypes(
                    loginUser.getPrivileges(), ";", "MANAGER", "ADMIN")) {
                throw PermissionInvalidException.invalidPermission("没有权限访问");
            }
        }
        @Override
        protected void auth(String requiredType, LoginUserVo loginUserVo) {
            String type = requiredType.toUpperCase();
            if (type.equals(Types.NONE)) {
                return;
            }
            this.authLogin(loginUserVo);
            if (type.equals(Types.ADMIN)) {
                this.authAdmin(loginUserVo);
            }
            if (type.equals(Types.MANAGER)) {
                this.authManager(loginUserVo);
            }
        }
    };

    public static class Types {
        /** 无需验证 */
        public static final String NONE = "NONE";
        /** 登陆用户 */
        public static final String LOGIN = "LOGIN";
        /** 超级管理员 */
        public static final String ADMIN = "ADMIN";
        /** 后台管理员 */
        public static final String MANAGER = "MANAGER";

        public static String[] values() {
            return new String[] {
                    NONE,
                    LOGIN,
                    ADMIN,
                    MANAGER
            };
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Type {
        String value() default Types.LOGIN;
    }

}
