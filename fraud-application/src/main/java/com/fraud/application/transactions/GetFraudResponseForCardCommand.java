package com.fraud.application.transactions;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;
import com.fraud.application.transactions.dto.FraudResponseDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Command requesting a fraud response for a card transaction. It is both the HTTP request body
 * and the CQRS message. The return type is carried generically: Command<ApiResult<FraudResponseDto>>.
 */
public record GetFraudResponseForCardCommand(
        @Positive int module,
        @Positive long transactionMessageId,
        @NotBlank String shadowCardNo,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotBlank String merchantId,
        @NotNull Instant transactionDate
) implements Command<ApiResult<FraudResponseDto>> {
}
