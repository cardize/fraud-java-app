package com.payguard.infrastructure.anomaly;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bir kartın çevrimiçi (online) istatistikleri: ortalama tutar, varyans, son işlem zamanları.
 *
 * Ortalama/varyans Welford algoritmasıyla artımlı (tüm geçmişi saklamadan) hesaplanır.
 */
public class CardStatistics {

    private long count;
    private double mean;
    private double m2;                 // Welford: varyans birikimi
    private final Deque<Instant> recentTimes = new ArrayDeque<>();

    public synchronized double mean() {
        return mean;
    }

    public synchronized double stdDev() {
        return count > 1 ? Math.sqrt(m2 / (count - 1)) : 0.0;
    }

    public synchronized long count() {
        return count;
    }

    /** Son 1 saatteki işlem sayısı (frekans göstergesi). */
    public synchronized long frequencyLastHour(Instant now) {
        return recentTimes.stream()
                .filter(t -> t.isAfter(now.minusSeconds(3600)))
                .count();
    }

    /** Yeni işlemi istatistiklere ekler (Welford güncellemesi). */
    public synchronized void update(double amount, Instant when) {
        count++;
        double delta = amount - mean;
        mean += delta / count;
        m2 += delta * (amount - mean);

        recentTimes.addLast(when);
        // pencereyi sınırla (bellek): son 100 işlem
        while (recentTimes.size() > 100) {
            recentTimes.removeFirst();
        }
    }
}
