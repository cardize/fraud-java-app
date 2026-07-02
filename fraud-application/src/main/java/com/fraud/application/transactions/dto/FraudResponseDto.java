package com.fraud.application.transactions.dto;

import java.util.UUID;

/**
 * Fraud response returned to the client.
 */
public record FraudResponseDto(
        UUID transactionId,
        String fraudResponseCode
) {
}
