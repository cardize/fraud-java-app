package com.payguard.application.transactions;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.Command;
import com.payguard.application.transactions.dto.FraudResponseDto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kart için fraud yanıtı isteyen komut. Hem API isteği hem CQRS mesajıdır.
 *
 * .NET karşılığı: RequestGetFraudResponseForCardCommand : IRequest<IDataResult<GetFraudResponseDto>>
 * (.NET'te request gövdesi doğrudan command sınıfıydı — aynı deseni koruyoruz.)
 *
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
