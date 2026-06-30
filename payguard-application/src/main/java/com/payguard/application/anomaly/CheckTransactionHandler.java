package com.payguard.application.anomaly;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * CheckTransactionCommand handler'ı — anomali kontrolünü port'a delege eder.
 */
@Component
public class CheckTransactionHandler
        implements CommandHandler<CheckTransactionCommand, ApiResult<AnomalyResult>> {

    private final AnomalyDetector anomalyDetector;

    public CheckTransactionHandler(AnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    @Override
    public ApiResult<AnomalyResult> handle(CheckTransactionCommand cmd) {
        AnomalyResult result = anomalyDetector.check(new FraudTransaction(
                cmd.transactionId(), cmd.shadowCardNo(), cmd.amount(),
                cmd.merchantId(), cmd.transactionDate()));
        return ApiResult.ok(result);
    }

    @Override
    public Class<CheckTransactionCommand> commandType() {
        return CheckTransactionCommand.class;
    }
}
