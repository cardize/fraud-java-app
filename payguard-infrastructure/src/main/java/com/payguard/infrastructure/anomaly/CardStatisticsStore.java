package com.payguard.infrastructure.anomaly;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kart istatistiklerini tutar (kart no -> istatistik).
 *
 * BUG DÜZELTMESİ: Önceki sürüm sınırsız bir ConcurrentHashMap kullanıyordu — her yeni kart
 * numarası kalıcı olarak belleğe ekleniyor, hiç tahliye edilmiyordu (uzun süre çalışan bir
 * prosesin belleği sürekli büyür, klasik bir memory leak). Caffeine ile boyut + erişim-sonrası
 * TTL sınırı konuldu; az kullanılan kartlar otomatik tahliye edilir.
 */
@Component
public class CardStatisticsStore {

    private final Cache<String, CardStatistics> byCard = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    public CardStatistics getOrCreate(String shadowCardNo) {
        return byCard.get(shadowCardNo, k -> new CardStatistics());
    }
}
