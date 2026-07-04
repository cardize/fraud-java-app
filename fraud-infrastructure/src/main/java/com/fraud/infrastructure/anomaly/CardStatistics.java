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

    /** Consistent, immutable view of the statistics at a single point in time. */
    public record Snapshot(long count, double mean, double stdDev, long frequencyLastHour) {}

    private final ReentrantLock lock = new ReentrantLock();

    private long count;
    private double mean;
    private double m2;                 // Welford: variance accumulator
    private final Deque<Instant> recentTimes = new ArrayDeque<>();

    /**
     * ATOMICITY FIX: captures a consistent snapshot of the statistics and applies the new
     * transaction in ONE critical section.
     *
     * The previous flow called mean()/stdDev()/count()/frequencyLastHour() as separate locked
     * calls and update() at the end — each call was individually safe, but the SEQUENCE was not:
     * a concurrent update could land between two reads, so the score could be computed from a mean
     * of one state and a stdDev of another (statistical drift under high concurrency for the same
     * card). Now the score inputs always describe a single consistent state, and the snapshot is
     * taken BEFORE this transaction is folded in (same semantics as the old read-then-update flow).
     */
    public Snapshot snapshotAndUpdate(double amount, Instant when) {
        lock.lock();
        try {
            Snapshot snapshot = new Snapshot(count, mean, stdDevLocked(), frequencyLastHourLocked(when));
            updateLocked(amount, when);
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

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
            return stdDevLocked();
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
            return frequencyLastHourLocked(now);
        } finally {
            lock.unlock();
        }
    }

    /** Adds a new transaction to the statistics (Welford update). */
    public void update(double amount, Instant when) {
        lock.lock();
        try {
            updateLocked(amount, when);
        } finally {
            lock.unlock();
        }
    }

    // ---- internals: MUST be called while holding the lock ----

    private double stdDevLocked() {
        return count > 1 ? Math.sqrt(m2 / (count - 1)) : 0.0;
    }

    private long frequencyLastHourLocked(Instant now) {
        return recentTimes.stream()
                .filter(t -> t.isAfter(now.minusSeconds(3600)))
                .count();
    }

    private void updateLocked(double amount, Instant when) {
        count++;
        double delta = amount - mean;
        mean += delta / count;
        m2 += delta * (amount - mean);

        recentTimes.addLast(when);
        // bound the window (memory): last 100 transactions
        while (recentTimes.size() > 100) {
            recentTimes.removeFirst();
        }
    }
}
