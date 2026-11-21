# metan

`metan` is a Java library for working with structured JSON-like values and composing results from multiple sources. It provides a typed `MetaJson` tree, path resolution and configurable merge operations.

## Main components

- **`MetaJson`** models nodes, arrays and scalar values, with methods for reading and modifying them.
- **`JsonPath`** and `JsonPathElement` identify values inside the model.
- **`JsonMerger`** applies merge instructions, including resolving, appending, putting and removing values.
- **Jackson and JDBC adapters** are located in `jackson/` and `jdbc/`.

The merge model is declared in `merger/Merger.java`; its actions and conditional structures live under `merger/`. The library is intended to be used from Java code rather than run as an application.

## Build

The repository contains a Gradle wrapper. From the repository root:

```sh
./gradlew build
```

Gradle configures JUnit 5 for tests and Maven publishing. The publishing destination is a separate package registry; publishing requires credentials supplied by the environment. You do not need publishing credentials just to inspect the code.

## Where to start

- [`MetaJson.java`](src/main/java/ru/council/metan/models/MetaJson.java): the tree representation.
- [`JsonMerger.java`](src/main/java/ru/council/metan/merger/JsonMerger.java): merge execution.
- [`Merger.java`](src/main/java/ru/council/metan/merger/Merger.java): merge configuration.
- [`MetaJsonRowMapper.java`](src/main/java/ru/council/metan/jdbc/MetaJsonRowMapper.java): JDBC integration.

**Note:** this README describes the checked-in API and build configuration. It does not claim a published public artifact or a tested end-to-end example.
