package com.payguard.application.common;

/**
 * Standart sonuç zarfı (başarı/veri/mesaj).
 *
 * .NET karşılığı: Core.Utilities.Results.IDataResult<T> / DataResult<T> / CreatedDataResult<T>
 * (Prisma framework'ünden gelen ortak dönüş tipi).
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
