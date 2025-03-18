package org.agentpower.service.secure.recognization;

public class PermissionInvalidException extends RuntimeException {
    private PermissionInvalidException() {
    }

    private PermissionInvalidException(String message) {
        super(message);
    }

    private PermissionInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PermissionInvalidException invalidPermission(String state) {
        return new PermissionInvalidException(state);
    }
}
