package org.agentpower.api;

import java.util.List;

/**
 * 通用返回结果
 * @param <T> 返回数据类型
 */
public record CommonResponse<T>(int code, String message, T data, List<T> rows, long total, long pageSize,
                                long pageNo) {
    public boolean isSuccess() {
        return code == Code.SUCCESS;
    }

    public static class Code {
        public static final int SUCCESS = 200;
        public static final int FAILURE = 500;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
    }


    public static <T> CommonResponse<T> success(T data) {
        return success("success", data);
    }

    public static <T> CommonResponse<T> success(String msg, T data) {
        return new CommonResponse<>(Code.SUCCESS, msg, data, null, 0, 0, 0);
    }

    public static <T> CommonResponse<T> success(List<T> rows) {
        return success("success", rows, rows.size(), rows.size(), 1);
    }

    public static <T> CommonResponse<T> success(List<T> rows, long total, long pageSize, long pageNo) {
        return success("success", rows, total, pageSize, pageNo);
    }

    public static <T> CommonResponse<T> success(String msg, List<T> rows, long total, long pageSize, long pageNo) {
        return new CommonResponse<>(Code.SUCCESS, msg, null, rows, total, pageSize, pageNo);
    }

    public static <T> CommonResponse<T> failure(String msg) {
        return new CommonResponse<>(Code.FAILURE, msg, null, null, 0, 0, 0);
    }

    public static <T> CommonResponse<T> unauthorized() {
        return new CommonResponse<>(Code.UNAUTHORIZED, "unauthorized", null, null, 0, 0, 0);
    }

    public static <T> CommonResponse<T> forbidden() {
        return forbidden("forbidden");
    }

    public static <T> CommonResponse<T> forbidden(String msg) {
        return new CommonResponse<>(Code.FORBIDDEN, msg, null, null, 0, 0, 0);
    }

}
