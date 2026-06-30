package com.payguard.infrastructure.anomaly;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kart istatistiklerini tutar (kart no -> istatistik).
 *
 * Bellek-içi (ConcurrentHashMap); üretimde DB/Redis'e taşınır.
 */
@Component
public class CardStatisticsStore {

    private final Map<String, CardStatistics> byCard = new ConcurrentHashMap<>();

    public CardStatistics getOrCreate(String shadowCardNo) {
        return byCard.computeIfAbsent(shadowCardNo, k -> new CardStatistics());
    }
}
