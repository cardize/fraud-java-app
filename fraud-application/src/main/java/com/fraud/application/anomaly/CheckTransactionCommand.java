package com.fraud.application.anomaly;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/**
 * Command that queries whether a transaction is anomalous.
 */
public record CheckTransactionCommand(
        @NotNull UUID transactionId,
        @NotBlank String shadowCardNo,
        @PositiveOrZero double amount,
        @NotBlank String merchantId,
        @NotNull Instant transactionDate
) implements Command<ApiResult<AnomalyResult>> {
}
