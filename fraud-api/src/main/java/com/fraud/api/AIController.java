package com.fraud.api;

import com.fraud.application.anomaly.AnomalyResult;
import com.fraud.application.anomaly.CheckTransactionCommand;
import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Mediator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI / Anomaly Detection", description = "Statistical anomaly scoring, independent of the rule engine")
public class AIController {

    private final Mediator mediator;

    public AIController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/check-transaction")
    @Operation(summary = "Score a transaction for anomaly", description = "Hybrid statistical "
            + "score (z-score, frequency, time-of-day) per card, kept in a bounded in-memory "
            + "cache. A card with fewer than 5 prior transactions is treated as suspicious by "
            + "default (insufficient history).")
    public ApiResult<AnomalyResult> checkTransaction(@Valid @RequestBody CheckTransactionCommand command) {
        return mediator.send(command);
    }
}
