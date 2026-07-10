package com.fraud.anomaly;

import com.fraud.infrastructure.anomaly.CardStatistics;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Welford-algorithm correctness plus a concurrency test that actually exercises the claim behind
 * {@link CardStatistics#snapshotAndUpdate}: that snapshot + update happen in one atomic step.
 *
 * The mean/stdDev correctness checks alone wouldn't catch a torn read; this class also fires many
 * threads at the SAME instance and checks the aggregate against an independently computed batch
 * formula — a lost or double-counted update would show up as a mismatch. This is the update
 * verifying the atomicity fix's own doc comment, the way DuplicateClaimConcurrencyTest verifies
 * claimMessage's.
 */
class CardStatisticsTest {

    @Test
    void meanAndStdDevMatchTheBatchFormula() {
        CardStatistics stats = new CardStatistics();
        double[] amounts = {100, 200, 300, 400, 500};
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        for (double amount : amounts) {
            stats.update(amount, now);
        }

        assertEquals(5, stats.count());
        assertEquals(300.0, stats.mean(), 1e-9);
        // sample standard deviation (N-1 denominator), matching CardStatistics.stdDevLocked()
        assertEquals(158.11388300841898, stats.stdDev(), 1e-6);
    }

    @Test
    void stdDevIsZeroWithFewerThanTwoSamples() {
        CardStatistics stats = new CardStatistics();
        assertEquals(0.0, stats.stdDev());

        stats.update(100, Instant.now());
        assertEquals(0.0, stats.stdDev(), "a single sample has no variance yet");
    }

    @Test
    void frequencyLastHourOnlyCountsRecentTransactions() {
        CardStatistics stats = new CardStatistics();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");

        stats.update(10, now.minusSeconds(30 * 60));   // 30 min ago -> counts
        stats.update(10, now.minusSeconds(90 * 60));   // 90 min ago -> too old
        stats.update(10, now);                          // now -> counts

        assertEquals(2, stats.frequencyLastHour(now));
    }

    @Test
    void snapshotAndUpdateReflectsStateBeforeThisTransaction() {
        CardStatistics stats = new CardStatistics();
        stats.update(100, Instant.now());
        stats.update(200, Instant.now());

        CardStatistics.Snapshot snapshot = stats.snapshotAndUpdate(999, Instant.now());

        assertEquals(2, snapshot.count(), "snapshot must describe the state BEFORE this call's own update");
        assertEquals(150.0, snapshot.mean(), 1e-9);
        assertEquals(3, stats.count(), "but the update itself must still have been applied");
    }

    /**
     * Fires many threads at ONE CardStatistics instance, each doing a single snapshotAndUpdate.
     * If snapshot+update were not atomic (the pre-fix behavior — separate locked calls with an
     * update at the end), a concurrent update landing between a read and the update could corrupt
     * the aggregate. Here we only assert on the FINAL state (order-independent for Welford), so a
     * lost or duplicated update is what this test would actually catch: count would be wrong, or
     * mean/stdDev would drift from the independently computed batch values.
     */
    @Test
    void concurrentSnapshotAndUpdateLosesNoUpdates() throws InterruptedException {
        CardStatistics stats = new CardStatistics();
        int threadCount = 50;
        List<Double> amounts = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            amounts.add(100.0 + i); // 100..149, distinct values so any drop/duplication is detectable
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();
        try {
            for (double amount : amounts) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        stats.snapshotAndUpdate(amount, Instant.now());
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                });
            }
            ready.await(5, TimeUnit.SECONDS);
            go.countDown(); // release all threads at once to maximize contention
            pool.shutdown();
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "all threads must finish");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, errors.get());
        assertEquals(threadCount, stats.count(), "every concurrent update must be counted exactly once");

        double expectedMean = amounts.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        assertEquals(expectedMean, stats.mean(), 1e-6);

        double sumSquaredDiff = amounts.stream().mapToDouble(a -> Math.pow(a - expectedMean, 2)).sum();
        double expectedStdDev = Math.sqrt(sumSquaredDiff / (threadCount - 1));
        assertEquals(expectedStdDev, stats.stdDev(), 1e-6);
    }
}
