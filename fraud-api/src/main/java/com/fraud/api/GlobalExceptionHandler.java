package com.fraud.api;

import com.fraud.application.common.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handler.
 *
 * SECURITY: Internal exceptions' stack traces and messages are NEVER leaked to the client; they
 * are logged and a generic message is returned. This prevents information disclosure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation failure -> 400 + per-field messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResult.fail("Validation error — " + details));
    }

    /**
     * Client-caused bad argument -> 400.
     *
     * Deliberately ONLY IllegalArgumentException (external review finding F):
     * IllegalStateException used to be mapped here too, but it almost always signals a
     * SERVER-side invariant violation — mapping it to 400 blamed the client and hid real 500s
     * from error-rate metrics and alerting. It now falls through to the generic 500 handler.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleBadRequest(RuntimeException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResult.fail("Invalid request"));
    }

    /** All uncaught errors -> 500; the detail is logged but never exposed. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("An unexpected error occurred"));
    }
}
