package com.payguard.application.transactions;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.CommandHandler;
import com.payguard.application.fraud.FraudParameters;
import com.payguard.application.fraud.ScenarioService;
import com.payguard.application.queue.OfflineOperation;
import com.payguard.application.queue.OfflineOperationPublisher;
import com.payguard.application.tenant.TenantProvider;
import com.payguard.application.transactions.dto.FraudResponseDto;
import com.payguard.domain.shared.ControlCode;
import com.payguard.domain.shared.ProductType;
import com.payguard.domain.transaction.Transaction;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The heart of the card fraud flow.
 *   1) Persist the transaction (generate a new TransactionId)
 *   2) Skip the fraud check if it's a duplicate
 *   3) Build FraudParameters
 *   4) Run the online scenarios SYNCHRONOUSLY -> fraudResponseCode (returned to the client)
 *   5) Write offline work to the outbox (without blocking the response)
 */
@Component
public class GetFraudResponseForCardHandler
        implements CommandHandler<GetFraudResponseForCardCommand, ApiResult<FraudResponseDto>> {

    private static final double DEFAULT_THRESHOLD = 5000.0;

    private final TransactionStore transactionStore;
    private final ScenarioService scenarioService;
    private final OfflineOperationPublisher offlinePublisher;
    private final MeterRegistry meterRegistry;
    private final TenantProvider tenantProvider;

    public GetFraudResponseForCardHandler(TransactionStore transactionStore,
                                          ScenarioService scenarioService,
                                          OfflineOperationPublisher offlinePublisher,
                                          MeterRegistry meterRegistry,
                                          TenantProvider tenantProvider) {
        this.transactionStore = transactionStore;
        this.scenarioService = scenarioService;
        this.offlinePublisher = offlinePublisher;
        this.meterRegistry = meterRegistry;
        this.tenantProvider = tenantProvider;
    }

    private void countDecision(String code) {
        meterRegistry.counter("payguard.fraud.decisions", "code", code).increment();
    }

    @Override
    @Transactional
    public ApiResult<FraudResponseDto> handle(GetFraudResponseForCardCommand cmd) {
        // 1) New transaction id + ATOMIC duplicate claim (race-free — see TransactionStore.claimMessage)
        UUID transactionId = UUID.randomUUID();

        boolean firstClaim = transactionStore.claimMessage(cmd.transactionMessageId(), cmd.module());
        ControlCode controlCode = firstClaim ? ControlCode.NORMAL : ControlCode.DUPLICATE;

        Transaction tx = new Transaction(
                transactionId, cmd.transactionMessageId(), cmd.module(),
                cmd.shadowCardNo(), cmd.amount(), cmd.merchantId(),
                cmd.transactionDate(), controlCode);

        // mark the previous row for the same message as "not latest"
        transactionStore.markPreviousAsNotLatest(cmd.transactionMessageId(), cmd.module(), transactionId);

        // 2) Skip the fraud check for duplicates
        if (controlCode == ControlCode.DUPLICATE) {
            tx.setFraudResponseCode("DUPLICATE");
            transactionStore.save(tx);
            countDecision("DUPLICATE");
            return ApiResult.ok(new FraudResponseDto(transactionId, "DUPLICATE"), "Duplicate transaction");
        }

        // 3) Rule engine parameters
        FraudParameters params = new FraudParameters(
                transactionId, cmd.shadowCardNo(), cmd.amount(),
                cmd.merchantId(), cmd.transactionDate(), DEFAULT_THRESHOLD);

        // 4) Online scenarios (synchronous) -> client response
        String fraudResponseCode = scenarioService.processOnlineScenarios(ProductType.CARD, cmd.module(), params);
        tx.setFraudResponseCode(fraudResponseCode);
        transactionStore.save(tx);

        // 5) Offline work — written to the outbox (inside this @Transactional, atomic with the business record)
        // BUGFIX: this used to always write the literal "default" tenant, ignoring the real tenant
        // carried by the X-Tenant header. It now reads the actual tenant from the TenantProvider port.
        offlinePublisher.publish(new OfflineOperation(
                transactionId, cmd.module(), fraudResponseCode, tenantProvider.currentTenant()));

        countDecision(fraudResponseCode);
        return ApiResult.ok(new FraudResponseDto(transactionId, fraudResponseCode));
    }

    @Override
    public Class<GetFraudResponseForCardCommand> commandType() {
        return GetFraudResponseForCardCommand.class;
    }
}
