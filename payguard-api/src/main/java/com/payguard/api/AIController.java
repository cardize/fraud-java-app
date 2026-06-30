package com.payguard.api;

import com.payguard.application.anomaly.AnomalyResult;
import com.payguard.application.anomaly.CheckTransactionCommand;
import com.payguard.application.cqrs.Mediator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI / anomali tespiti uç noktaları.
 * Controller ince: komutu Mediator'a verir (CQRS).
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
