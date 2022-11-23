package fr.dynamx.api.network.sync.v3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MapSynchronizedVariable<K, V> extends SynchronizedEntityVariable<Map<K, V>> {
    public MapSynchronizedVariable(SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<Map<K, V>> serializer, String name) {
        super(null, synchronizationRule, serializer, new HashMap<>(), name);
    }

    public MapSynchronizedVariable(BiConsumer<SynchronizedEntityVariable<Map<K, V>>, Map<K, V>> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<Map<K, V>> serializer, String name) {
        super(receiveCallback, synchronizationRule, serializer, new HashMap<>(), name);
    }

    public void put(K key, V value) {
        // if already changed, or if the new value has significantly changed
        if (changed || !Objects.equals(value, get().get(key))) {
            get().put(key, value);
            setChanged(true);
        }
    }
}
