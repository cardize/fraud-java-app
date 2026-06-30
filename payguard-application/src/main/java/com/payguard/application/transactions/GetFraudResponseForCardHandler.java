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
 * Kart fraud akışının kalbi.
 *   1) İşlemi kaydet (yeni TransactionId üret)
 *   2) Duplicate ise fraud kontrolünü atla
 *   3) FraudParameters oluştur
 *   4) Online senaryoları SENKRON çalıştır -> fraudResponseCode (istemciye döner)
 *   5) Offline işlemleri outbox'a yaz (yanıtı bekletmeden)
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
        // 1) Yeni işlem kimliği + ATOMİK duplicate claim (race-free — bkz. TransactionStore.claimMessage)
        UUID transactionId = UUID.randomUUID();

        boolean firstClaim = transactionStore.claimMessage(cmd.transactionMessageId(), cmd.module());
        ControlCode controlCode = firstClaim ? ControlCode.NORMAL : ControlCode.DUPLICATE;

        Transaction tx = new Transaction(
                transactionId, cmd.transactionMessageId(), cmd.module(),
                cmd.shadowCardNo(), cmd.amount(), cmd.merchantId(),
                cmd.transactionDate(), controlCode);

        // önceki aynı mesajı "latest değil" yap
        transactionStore.markPreviousAsNotLatest(cmd.transactionMessageId(), cmd.module(), transactionId);

        // 2) Duplicate ise fraud kontrolü yapılmaz
        if (controlCode == ControlCode.DUPLICATE) {
            tx.setFraudResponseCode("DUPLICATE");
            transactionStore.save(tx);
            countDecision("DUPLICATE");
            return ApiResult.ok(new FraudResponseDto(transactionId, "DUPLICATE"), "Duplicate işlem");
        }

        // 3) Kural motoru parametreleri
        FraudParameters params = new FraudParameters(
                transactionId, cmd.shadowCardNo(), cmd.amount(),
                cmd.merchantId(), cmd.transactionDate(), DEFAULT_THRESHOLD);

        // 4) Online senaryolar (senkron) -> istemci yanıtı
        String fraudResponseCode = scenarioService.processOnlineScenarios(ProductType.CARD, cmd.module(), params);
        tx.setFraudResponseCode(fraudResponseCode);
        transactionStore.save(tx);

        // 5) Offline işlemler — outbox'a yazılır (bu @Transactional içinde, iş kaydıyla atomik)
        // BUG DÜZELTMESİ: önceden tenant her zaman sabit "default" yazılıyordu; X-Tenant header'ı
        // ile gelen gerçek kiracı bilgisi yok sayılıyordu. Artık TenantProvider port'undan okunuyor.
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
