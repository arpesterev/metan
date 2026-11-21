# Concurrency and object ownership

`MetaJson` is an intentionally mutable data model and must not be modified concurrently by different threads without coordination. To share a tree safely, use the new **`SynchronizedMetaJson`** wrapper.

## Safe shared-state API

```java
MetaJson initial = new MetaJson(JsonElementType.Array);
SynchronizedMetaJson shared = new SynchronizedMetaJson(initial);

// Each update is atomic and isolated from other updates.
shared.update(tree -> tree.add(
    new MetaJson(JsonElementType.Scalar, new Scalar<>(42))));

// Returns a detached copy. Mutating this copy does not change shared state.
MetaJson snapshot = shared.snapshot();
```

The wrapper copies the seed, works on a private copy during each update and copies the result before committing. If a callback fails, no partial update is committed. The callback can retain its working copy without gaining access to the stored state. `merge(configuration, incoming)` offers the same serialized update semantics, assuming the caller does not concurrently mutate `incoming` or its configuration.

This copy-on-update approach trades memory and CPU for correctness. Do not use it without measuring performance for large trees. It is thread-safe within a single JVM object, not a distributed transactional store.

## Copy semantics

[`MetaJsonSnapshots.copy()`](../src/main/java/ru/council/metan/models/MetaJsonSnapshots.java) recursively copies nodes, arrays and parent links. Only explicitly recognized immutable scalar types, including common `java.time` value classes, are reused. JDBC date/time values, regular dates and primitive arrays are copied. Mutable builders such as `DateTimeFormatterBuilder` are rejected even though they live under the `java.time` package. Unsupported mutable scalar objects are **rejected**, not silently aliased. Cycles in child links are rejected. The source tree must not be modified during copying unless it is protected by the wrapper.

## Remaining unsafe APIs

- [`MetaJson`](../src/main/java/ru/council/metan/models/MetaJson.java) uses `HashMap` and `ArrayList`; `getNode()` and `getArray()` expose them directly. An unwrapped shared tree is not safe for concurrent writes.
- [`JsonMerger.mergeJsons()`](../src/main/java/ru/council/metan/merger/JsonMerger.java) mutates its target; use independent trees or the wrapper.
- [`MetaJsonRowMapper`](../src/main/java/ru/council/metan/jdbc/MetaJsonRowMapper.java) accumulates results in instance fields. Use a new mapper for every query.
- `JsonPath` is mutable. Keep paths request-scoped.

Do not configure Jackson `ObjectMapper` instances while they are in use by other threads.

## Verification

[`SynchronizedMetaJsonTest`](../src/test/java/ru/council/metan/models/SynchronizedMetaJsonTest.java) exercises concurrent updates, detached snapshots, rollback and mutable scalar handling. Build execution status must be checked in CI; the documentation does not claim unrelated raw `MetaJson` operations are synchronized.
