package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.utils.debug.SyncTracker;

import java.util.function.BiConsumer;

public class EntityFloatArrayVariable extends EntityVariable<float[]>
{
    public EntityFloatArrayVariable(SynchronizationRules synchronizationRule, float[] initialValue) {
        super(synchronizationRule, initialValue);
    }

    public EntityFloatArrayVariable(BiConsumer<EntityVariable<float[]>, float[]> receiveCallback, SynchronizationRules synchronizationRule, float[] initialValue) {
        super(receiveCallback, synchronizationRule, initialValue);
    }

    public float get(int i) {
        return get()[i];
    }

    public void set(int i, float value) {
        // if already changed, or if the new value has significantly changed
        if (changed || SyncTracker.different(get()[i], value)) {
            get()[i] = value;
            setChanged(true);
        }
    }
}
