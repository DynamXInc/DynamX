package fr.dynamx.api.network.sync.v3;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import net.minecraftforge.fml.relauncher.Side;

import java.util.function.BiConsumer;

public class SynchronizedEntityVariable<T> {
    private final BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback;
    private final SynchronizationRules synchronizationRule;
    private final SynchronizedVariableSerializer<T> serializer;
    private T value;
    private boolean changed = true; //first sync

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, T initialValue) {
        this(null, synchronizationRule, null, initialValue);
    }

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer) {
        this(null, synchronizationRule, serializer, null);
    }

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, T initialValue) {
        this(null, synchronizationRule, serializer, initialValue);
    }

    public SynchronizedEntityVariable(BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer) {
        this(receiveCallback, synchronizationRule, serializer, null);
    }

    public SynchronizedEntityVariable(BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, T initialValue) {
        this.receiveCallback = receiveCallback;
        this.synchronizationRule = synchronizationRule;
        this.value = initialValue;

        if(serializer == null && initialValue != null) {
            this.serializer = (SynchronizedVariableSerializer<T>) SynchronizedEntityVariableFactory.getSerializer(initialValue.getClass());
        } else if(serializer != null) {
            this.serializer = serializer;
        } else {
            throw new IllegalArgumentException("Must set a serializer or a initial value");
        }
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        if(value != this.value) {
            this.value = value;
            changed = true;
        }
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void receiveValue(T value) {
        if(receiveCallback != null)
            receiveCallback.accept(this, value);
        this.value = value;
    }

    public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
        return changed ? synchronizationRule.getSyncTarget(simulationHolder, side) : SyncTarget.NONE;
    }

    public SynchronizedVariableSerializer<T> getSerializer() {
        return serializer;
    }
}
