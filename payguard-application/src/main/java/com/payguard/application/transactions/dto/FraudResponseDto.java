package com.payguard.application.transactions.dto;

import java.util.UUID;

/**
 * İstemciye dönen fraud yanıtı.
 */
public record FraudResponseDto(
        UUID transactionId,
        String fraudResponseCode
) {
}
