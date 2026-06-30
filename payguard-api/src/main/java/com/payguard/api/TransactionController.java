package com.payguard.api;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.Mediator;
import com.payguard.application.transactions.GetFraudResponseForCardCommand;
import com.payguard.application.transactions.dto.FraudResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşlem (fraud) uç noktaları.
 *
 * Controller ince kalır: sadece Mediator'a delege eder.
 * İleride PF / PayCell / TrKart uç noktaları aynı şekilde eklenir.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final Mediator mediator;

    public TransactionController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/get-fraud-response-for-card")
    public ApiResult<FraudResponseDto> getFraudResponseForCard(
            @Valid @RequestBody GetFraudResponseForCardCommand command) {
        return mediator.send(command);
    }
}
