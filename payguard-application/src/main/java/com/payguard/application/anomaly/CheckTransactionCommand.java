package com.payguard.application.anomaly;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/**
 * Bir işlemin anomali olup olmadığını sorgulayan komut.
 */
public record CheckTransactionCommand(
        @NotNull UUID transactionId,
        @NotBlank String shadowCardNo,
        @PositiveOrZero double amount,
        @NotBlank String merchantId,
        @NotNull Instant transactionDate
) implements Command<ApiResult<AnomalyResult>> {
}
