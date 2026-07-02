package com.fraud.infrastructure.anomaly;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A card's online statistics: mean amount, variance, recent transaction times.
 *
 * Mean/variance are computed incrementally with Welford's algorithm (without storing the full
 * history). Concurrency is guarded with a ReentrantLock (instead of synchronized, so virtual
 * threads are never pinned).
 */
public class CardStatistics {

    private final ReentrantLock lock = new ReentrantLock();

    private long count;
    private double mean;
    private double m2;                 // Welford: variance accumulator
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

    /** Number of transactions in the last hour (frequency indicator). */
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

    /** Adds a new transaction to the statistics (Welford update). */
    public void update(double amount, Instant when) {
        lock.lock();
        try {
            count++;
            double delta = amount - mean;
            mean += delta / count;
            m2 += delta * (amount - mean);

            recentTimes.addLast(when);
            // bound the window (memory): last 100 transactions
            while (recentTimes.size() > 100) {
                recentTimes.removeFirst();
            }
        } finally {
            lock.unlock();
        }
    }
}
