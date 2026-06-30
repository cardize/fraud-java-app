package com.payguard.api;

import com.payguard.application.common.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Merkezi hata yakalayıcı.
 *
 * GÜVENLİK: İç istisnaların stack-trace'i ve mesajı istemciye SIZDIRILMAZ; loglanır, istemciye
 * jenerik bir mesaj döner. Böylece bilgi ifşası (information disclosure) engellenir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** İstemci kaynaklı hatalı argüman → 400. */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResult<Void>> handleBadRequest(RuntimeException ex) {
        log.warn("Geçersiz istek: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResult.fail("Geçersiz istek"));
    }

    /** Yakalanmamış tüm hatalar → 500, detay loglanır ama dışarı verilmez. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("Beklenmeyen bir hata oluştu"));
    }
}
