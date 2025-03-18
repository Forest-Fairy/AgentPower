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

    private static final AuthRequired Default = new AuthRequired(
            Arrays.stream(Types.values()).map(Types::name).toArray(String[]::new)) {

        void authLogin(LoginUserVo loginUser) {
            if (StringUtils.isBlank(loginUser.getId())) {
                throw PermissionInvalidException.invalidPermission("用户未登录");
            }
        }
        void authAdmin(LoginUserVo loginUser) {
            if (loginUser.getRole() == null || ! SearchUtils.containsAnyTypes(
                    loginUser.getRole(), ";", "ADMIN")) {
                throw PermissionInvalidException.invalidPermission("没有权限访问");
            }
        }
        void authManager(LoginUserVo loginUser) {
            if (loginUser.getRole() == null || ! SearchUtils.containsAnyTypes(
                    loginUser.getRole(), ";", "MANAGER", "ADMIN")) {
                throw PermissionInvalidException.invalidPermission("没有权限访问");
            }
        }
        @Override
        protected void auth(String requiredType, LoginUserVo loginUserVo) {
            String type = requiredType.toUpperCase();
            if (type.equals(Types.NONE.name())) {
                return;
            }
            this.authLogin(loginUserVo);
            if (type.equals(Types.ADMIN.name())) {
                this.authAdmin(loginUserVo);
            }
            if (type.equals(Types.MANAGER.name())) {
                this.authManager(loginUserVo);
            }
        }
    };

    public enum Types {
        /** 无需验证 */
        NONE,
        /** 登陆用户 */
        LOGIN_BY_DEFAULT,
        /** 超级管理员 */
        ADMIN,
        /** 后台管理员 */
        MANAGER,
        ;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Type {
        String value();
    }

}
