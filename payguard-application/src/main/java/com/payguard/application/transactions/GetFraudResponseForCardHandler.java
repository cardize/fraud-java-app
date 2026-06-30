package com.payguard.application.transactions;

import com.payguard.application.common.ApiResult;
import com.payguard.application.cqrs.CommandHandler;
import com.payguard.application.fraud.FraudParameters;
import com.payguard.application.fraud.ScenarioService;
import com.payguard.application.queue.OfflineOperation;
import com.payguard.application.queue.OfflineOperationPublisher;
import com.payguard.application.transactions.dto.FraudResponseDto;
import com.payguard.domain.shared.ControlCode;
import com.payguard.domain.shared.ProductType;
import com.payguard.domain.transaction.Transaction;
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

    public GetFraudResponseForCardHandler(TransactionStore transactionStore,
                                          ScenarioService scenarioService,
                                          OfflineOperationPublisher offlinePublisher) {
        this.transactionStore = transactionStore;
        this.scenarioService = scenarioService;
        this.offlinePublisher = offlinePublisher;
    }

    @Override
    @Transactional
    public ApiResult<FraudResponseDto> handle(GetFraudResponseForCardCommand cmd) {
        // 1) Yeni işlem kimliği + kayıt
        UUID transactionId = UUID.randomUUID();

        ControlCode controlCode = transactionStore
                .existsByMessageIdAndModule(cmd.transactionMessageId(), cmd.module())
                ? ControlCode.DUPLICATE
                : ControlCode.NORMAL;

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
        offlinePublisher.publish(new OfflineOperation(transactionId, cmd.module(), fraudResponseCode, "default"));

        return ApiResult.ok(new FraudResponseDto(transactionId, fraudResponseCode));
    }

    @Override
    public Class<GetFraudResponseForCardCommand> commandType() {
        return GetFraudResponseForCardCommand.class;
    }
}
