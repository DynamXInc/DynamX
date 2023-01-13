package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class EntityMapVariable<T extends Map<K, V>, K, V> extends EntityVariable<T> {
    public EntityMapVariable(SynchronizationRules synchronizationRule) {
        this(null, synchronizationRule);
    }

    public EntityMapVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule) {
        super(receiveCallback, synchronizationRule, (T) new HashMap<>());
    }

    public void put(K key, V value) {
        // if already changed, or if the new value has significantly changed
        if (changed || !Objects.equals(value, get().get(key))) {
            get().put(key, value);
            setChanged(true);
        }
    }
}
