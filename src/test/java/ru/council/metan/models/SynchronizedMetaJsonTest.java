package ru.council.metan.models;

import org.junit.jupiter.api.Test;
import ru.council.metan.enums.JsonElementType;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizedMetaJsonTest {

    private static MetaJson scalar(int value) {
        return new MetaJson(JsonElementType.Scalar, new Scalar<>(value));
    }

    @Test
    void concurrentAppendsAreSerialized() throws Exception {
        SynchronizedMetaJson shared =
                new SynchronizedMetaJson(new MetaJson(JsonElementType.Array));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 180; i++) {
                final int number = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    shared.update(root -> root.add(scalar(number)));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
            MetaJson snapshot = shared.snapshot();
            assertEquals(180, snapshot.getArray().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void callerCannotMutateInternalTreeThroughSnapshots() {
        MetaJson seed = new MetaJson(JsonElementType.Node);
        MetaJson list = new MetaJson(JsonElementType.Array);
        list.add(scalar(1));
        seed.put("items", list);
        SynchronizedMetaJson shared = new SynchronizedMetaJson(seed);

        seed.resolve("items").add(scalar(2));
        MetaJson outside = shared.snapshot();
        outside.resolve("items").add(scalar(3));

        assertEquals(1, shared.snapshot().resolve("items").getArray().size());
        assertSame(outside, outside.resolve("items").getParent());
    }

    @Test
    void failedUpdateDoesNotCommitPartialMutation() {
        SynchronizedMetaJson shared = new SynchronizedMetaJson(new MetaJson(JsonElementType.Array));
        assertThrows(IllegalStateException.class,
                () -> shared.update(root -> {
                    root.add(scalar(1));
                    throw new IllegalStateException("rollback");
                }));
        assertTrue(shared.snapshot().getArray().isEmpty());
    }

    @Test
    void unsupportedMutableValuesAreRejected() {
        MetaJson scalar = new MetaJson(JsonElementType.Scalar, new Scalar<>(new StringBuilder("mutable")));
        assertThrows(IllegalArgumentException.class, () -> new SynchronizedMetaJson(scalar));
    }

    @Test
    void mutableJavaTimeBuilderCannotEscapeTheSnapshotBoundary() {
        MetaJson mutableValue = new MetaJson(
                JsonElementType.Scalar,
                new Scalar<>(new java.time.format.DateTimeFormatterBuilder().appendLiteral('x')));
        assertThrows(IllegalArgumentException.class, () -> new SynchronizedMetaJson(mutableValue));
    }

    @Test
    void immutableJavaTimeScalarsRemainSupported() {
        java.time.LocalDate date = java.time.LocalDate.of(2026, 11, 21);
        SynchronizedMetaJson shared = new SynchronizedMetaJson(
                new MetaJson(JsonElementType.Scalar, new Scalar<>(date)));
        assertEquals(date, shared.snapshot().getScalar().getValue());
    }

    @Test
    void mutableJdbcTimestampsAreCopied() {
        Timestamp original = Timestamp.valueOf("2026-11-21 12:30:45.123456789");
        SynchronizedMetaJson shared = new SynchronizedMetaJson(
                new MetaJson(JsonElementType.Scalar, new Scalar<>(original)));
        original.setNanos(1);
        Timestamp saved = (Timestamp) shared.snapshot().getScalar().getValue();
        assertEquals(123456789, saved.getNanos());
        saved.setNanos(2);
        assertEquals(123456789, ((Timestamp) shared.snapshot().getScalar().getValue()).getNanos());
    }
}
