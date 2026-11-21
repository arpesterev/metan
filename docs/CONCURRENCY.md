# Concurrency and object ownership

**Status:** `MetaJson` and `MetaJsonRowMapper` are mutable and **not safe for concurrent access when any caller may modify their state**. The library does not synchronize access to a shared result tree.

## Where the risk comes from

- [`MetaJson`](../src/main/java/ru/council/metan/models/MetaJson.java) stores its node/array contents in ordinary `HashMap` / `ArrayList` instances. `getNode()` and `getArray()` return the actual collections. Calls to `put()`, `add()`, `remove()`, setters and direct collection mutations affect the shared object.
- [`JsonMerger.mergeJsons()`](../src/main/java/ru/council/metan/merger/JsonMerger.java) and `processFusion()` modify the supplied target tree. These methods use local temporary variables, but that does **not** make concurrently modifying the same target safe.
- [`MetaJsonRowMapper`](../src/main/java/ru/council/metan/jdbc/MetaJsonRowMapper.java) accumulates rows in instance fields `json` and `listOfJsons`. Use a **new mapper per query execution**, not one shared across simultaneous queries.
- `JsonPath` is a mutable path builder. Avoid sharing a path under concurrent modifications. [`JsonPath.add()`](../src/main/java/ru/council/metan/jsonpath/JsonPath.java) changes elements in the path.

## Safe usage boundaries

1. Build a new `MetaJson` graph for each independent request or operation. Keep merge destinations owned by one request.
2. For multiple readers, publish a fully constructed graph safely and guarantee that **no code modifies it**, including through the returned collections or nested values.
3. When a result must be shared and updated, guard the **whole read/modify/write operation** with a common lock, or design immutable snapshots. Switching the top-level map to `ConcurrentHashMap` alone would not protect arrays, parent pointers, or multi-step merges.
4. Do not reconfigure a Jackson `ObjectMapper` with `MetaJsonStdDeserializer.registerToObjectMapperInstance()` while other threads are using that mapper. Complete configuration before concurrent serialization/deserialization.

## Verification still needed

There are no concurrency guarantees or stress-test results established by this document. To claim parallel-write support, add tests with repeated concurrent `put()`, `add()`, `remove()` and `JsonMerger.mergeJsons()` calls on shared graphs, including nested structures, and specify the intended atomicity semantics.

This note describes the checked-in source, not a verified concurrency test run.
