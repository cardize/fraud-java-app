package com.payguard.application.common;

/**
 * Standart API sonuç zarfı (başarı/veri/mesaj).
 */
public record ApiResult<T>(boolean success, T data, String message) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(true, data, message);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(false, null, message);
    }
}
