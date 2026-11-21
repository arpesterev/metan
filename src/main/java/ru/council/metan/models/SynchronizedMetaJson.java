package ru.council.metan.models;

import ru.council.metan.merger.JsonMerger;
import ru.council.metan.merger.Merger;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Serializes modifications of a MetaJson tree using detached snapshots.
 *
 * <p>No reference to the internal tree escapes: the seed, callback input,
 * and returned snapshots are all independent copies. An update is committed
 * only if the callback and second copy both complete successfully.
 *
 * <p>All shared access must go through this wrapper. Plain MetaJson remains
 * a mutable, non-thread-safe data model.
 */
public final class SynchronizedMetaJson {
    private MetaJson state;

    public SynchronizedMetaJson(MetaJson initialState) {
        state = MetaJsonSnapshots.copy(initialState);
    }

    public synchronized MetaJson snapshot() {
        return MetaJsonSnapshots.copy(state);
    }

    public synchronized void update(Consumer<MetaJson> mutation) {
        Objects.requireNonNull(mutation, "mutation");
        MetaJson working = MetaJsonSnapshots.copy(state);
        mutation.accept(working);
        state = MetaJsonSnapshots.copy(working);
    }

    public void merge(Merger configuration, MetaJson incoming) {
        Objects.requireNonNull(configuration, "configuration");
        MetaJson source = MetaJsonSnapshots.copy(incoming);
        update(target -> JsonMerger.mergeJsons(configuration, target, source));
    }
}
