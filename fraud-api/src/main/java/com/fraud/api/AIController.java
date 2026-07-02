package com.fraud.api;

import com.fraud.application.anomaly.AnomalyResult;
import com.fraud.application.anomaly.CheckTransactionCommand;
import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Mediator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI / anomaly detection endpoints.
 * The controller stays thin: it hands the command to the Mediator (CQRS).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final Mediator mediator;

    public AIController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/check-transaction")
    public ApiResult<AnomalyResult> checkTransaction(@Valid @RequestBody CheckTransactionCommand command) {
        return mediator.send(command);
    }
}
