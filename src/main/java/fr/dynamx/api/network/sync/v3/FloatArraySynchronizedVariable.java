package fr.dynamx.api.network.sync.v3;

import fr.dynamx.utils.debug.SyncTracker;

import java.util.function.BiConsumer;

public class FloatArraySynchronizedVariable extends SynchronizedEntityVariable<float[]>
{
    public FloatArraySynchronizedVariable(SynchronizationRules synchronizationRule, String name) {
        super(synchronizationRule, SynchronizedEntityVariableFactory.floatArraySerializer, name);
    }

    public FloatArraySynchronizedVariable(SynchronizationRules synchronizationRule, float[] initialValue, String name) {
        super(synchronizationRule, SynchronizedEntityVariableFactory.floatArraySerializer, initialValue, name);
    }

    public FloatArraySynchronizedVariable(BiConsumer<SynchronizedEntityVariable<float[]>, float[]> receiveCallback, SynchronizationRules synchronizationRule, String name) {
        super(receiveCallback, synchronizationRule, SynchronizedEntityVariableFactory.floatArraySerializer, name);
    }

    public FloatArraySynchronizedVariable(BiConsumer<SynchronizedEntityVariable<float[]>, float[]> receiveCallback, SynchronizationRules synchronizationRule, float[] initialValue, String name) {
        super(receiveCallback, synchronizationRule, SynchronizedEntityVariableFactory.floatArraySerializer, initialValue, name);
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
