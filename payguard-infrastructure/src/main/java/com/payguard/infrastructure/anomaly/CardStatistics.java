package com.payguard.infrastructure.anomaly;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bir kartın çevrimiçi (online) istatistikleri: ortalama tutar, varyans, son işlem zamanları.
 *
 * Ortalama/varyans Welford algoritmasıyla artımlı (tüm geçmişi saklamadan) hesaplanır.
 * Eşzamanlılık ReentrantLock ile sağlanır (sanal thread'leri pinlememek için synchronized yerine).
 */
public class CardStatistics {

    private final ReentrantLock lock = new ReentrantLock();

    private long count;
    private double mean;
    private double m2;                 // Welford: varyans birikimi
    private final Deque<Instant> recentTimes = new ArrayDeque<>();

    public double mean() {
        lock.lock();
        try {
            return mean;
        } finally {
            lock.unlock();
        }
    }

    public double stdDev() {
        lock.lock();
        try {
            return count > 1 ? Math.sqrt(m2 / (count - 1)) : 0.0;
        } finally {
            lock.unlock();
        }
    }

    public long count() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /** Son 1 saatteki işlem sayısı (frekans göstergesi). */
    public long frequencyLastHour(Instant now) {
        lock.lock();
        try {
            return recentTimes.stream()
                    .filter(t -> t.isAfter(now.minusSeconds(3600)))
                    .count();
        } finally {
            lock.unlock();
        }
    }

    /** Yeni işlemi istatistiklere ekler (Welford güncellemesi). */
    public void update(double amount, Instant when) {
        lock.lock();
        try {
            count++;
            double delta = amount - mean;
            mean += delta / count;
            m2 += delta * (amount - mean);

            recentTimes.addLast(when);
            // pencereyi sınırla (bellek): son 100 işlem
            while (recentTimes.size() > 100) {
                recentTimes.removeFirst();
            }
        } finally {
            lock.unlock();
        }
    }
}
