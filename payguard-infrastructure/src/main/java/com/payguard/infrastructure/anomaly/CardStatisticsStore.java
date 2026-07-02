package com.payguard.infrastructure.anomaly;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Holds card statistics (card number -> statistics).
 *
 * BUGFIX: The previous version used an unbounded ConcurrentHashMap — every new card number was
 * added to memory permanently and never evicted (a classic memory leak in a long-running process).
 * Caffeine now enforces a size + access-based TTL limit; rarely used cards are evicted automatically.
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
