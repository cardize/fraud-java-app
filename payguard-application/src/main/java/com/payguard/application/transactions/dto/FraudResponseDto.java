package com.payguard.application.transactions.dto;

import java.util.UUID;

/**
 * İstemciye dönen fraud yanıtı.
 *
 * .NET karşılığı: PayGuard.Application/Handlers/Transactions/Dtos/GetFraudResponseDto.
 */
public record FraudResponseDto(
        UUID transactionId,
        String fraudResponseCode
) {
}
