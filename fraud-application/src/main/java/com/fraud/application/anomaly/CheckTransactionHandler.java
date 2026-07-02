package com.fraud.application.anomaly;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Handler for CheckTransactionCommand — delegates the anomaly check to the port.
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
