package com.payguard.application.transactions;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.Command;
import com.payguard.application.transactions.dto.FraudResponseDto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kart için fraud yanıtı isteyen komut. Hem HTTP istek gövdesi hem CQRS mesajıdır.
 * Dönüş tipi generic ile taşınır: Command<ApiResult<FraudResponseDto>>.
 */
public record GetFraudResponseForCardCommand(
        int module,
        long transactionMessageId,
        String shadowCardNo,
        BigDecimal amount,
        String merchantId,
        Instant transactionDate
) implements Command<ApiResult<FraudResponseDto>> {
}
