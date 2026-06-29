package com.payguard.application.anomaly;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.Command;

import java.time.Instant;
import java.util.UUID;

/**
 * Bir işlemin anomali olup olmadığını sorgulayan komut.
 *
 * .NET karşılığı: PayGuard.Application.AI/Handlers/AnomalyDetections/Commands/CheckTransactionCommand.
 */
public record CheckTransactionCommand(
        UUID transactionId,
        String shadowCardNo,
        double amount,
        String merchantId,
        Instant transactionDate
) implements Command<ApiResult<AnomalyResult>> {
}
