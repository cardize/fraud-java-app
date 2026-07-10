package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Mediator;
import com.fraud.application.transactions.GetFraudResponseForCardCommand;
import com.fraud.application.transactions.dto.FraudResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction (fraud) endpoints.
 *
 * The controller stays thin: it only delegates to the Mediator.
 * Future PF / PayCell / TrKart endpoints are added the same way.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Synchronous fraud decisions")
public class TransactionController {

    private final Mediator mediator;

    public TransactionController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/get-fraud-response-for-card")
    @Operation(summary = "Get a fraud response for a card transaction", description = "Runs the "
            + "online scenario engine synchronously and returns APPROVE/REJECT/REVIEW/DUPLICATE. "
            + "The same transactionMessageId+module is deduplicated atomically (see "
            + "TransactionStore.claimMessage). Offline work (outbox) is queued in the same "
            + "transaction, not on this response path.")
    public ApiResult<FraudResponseDto> getFraudResponseForCard(
            @Valid @RequestBody GetFraudResponseForCardCommand command) {
        return mediator.send(command);
    }
}
