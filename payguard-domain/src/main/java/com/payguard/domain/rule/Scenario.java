package com.payguard.domain.rule;

import java.io.Serializable;
import java.util.List;

/**
 * Bir senaryo: sıralı/öncelikli kurallar kümesi ve tetiklenince dönecek fraud yanıt kodu.
 *
 * .NET karşılığı: PayGuard.Domain/AggregateRoots/ScenarioDefinition.cs + ScenarioInfo (motor modeli).
 *
 * @param id              senaryo kimliği
 * @param name            ad
 * @param priority        öncelik (küçük = önce; birden çok hit olursa en yüksek öncelikli kazanır)
 * @param fraudResponseCode  senaryo tetiklenirse dönecek kod (örn "REJECT", "REVIEW")
 * @param rules           senaryodaki kurallar (hepsi true ise senaryo "hit")
 */
public record Scenario(
        long id,
        String name,
        int priority,
        String fraudResponseCode,
        List<Rule> rules
) implements Serializable {
}
