package fr.dynamx.api.network.sync.v3;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.utils.debug.SyncTracker;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.BiConsumer;

public class SynchronizedEntityVariable<T> {
    @Getter
    private final BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback;
    @Getter
    private final SynchronizationRules synchronizationRule;
    @Getter
    private final SynchronizedVariableSerializer<T> serializer;
    private T value;
    protected boolean changed = true; //first sync
    @Getter
    private final String name;

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, T initialValue, String name) {
        this(null, synchronizationRule, null, initialValue, name);
    }

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, String name) {
        this(null, synchronizationRule, serializer, null, name);
    }

    public SynchronizedEntityVariable(SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, T initialValue, String name) {
        this(null, synchronizationRule, serializer, initialValue, name);
    }

    public SynchronizedEntityVariable(BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, String name) {
        this(receiveCallback, synchronizationRule, serializer, null, name);
    }

    public SynchronizedEntityVariable(BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, T initialValue, String name) {
        this.receiveCallback = receiveCallback;
        this.synchronizationRule = synchronizationRule;
        this.value = initialValue;
        this.name = name;

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
        // todo wtf here
        if((value instanceof Float && SyncTracker.different((Float) value, (Float) this.value)) || (!(value instanceof Float) && value != this.value)) {
            this.value = value;
            changed = true;
            //System.out.println("SET " + value);
        }
    }

    public void setChanged(boolean changed) {
        //System.out.println("Mark change " + value+" "+this);
        this.changed = changed;
    }

    public void receiveValue(T value) {
        //System.out.println("RCV " + value+" in " +this);
        if(receiveCallback != null)
            receiveCallback.accept(this, value);
        if(value instanceof Map) { //TODO PUT IN SEPARATE CLASS
            ((Map)this.value).clear();
            ((Map)this.value).putAll((Map) value);
        } else
            this.value = value;
    }

    public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
        return changed ? synchronizationRule.getSyncTarget(simulationHolder, side) : SyncTarget.NONE;
    }

    public void writeValue(ByteBuf buffer, boolean lightData) {
        serializer.writeObject(buffer, get());
    }

    @Override
    public String toString() {
        return "SynchronizedEntityVariable{" +
                "receiveCallback=" + receiveCallback +
                ", synchronizationRule=" + synchronizationRule +
                ", serializer=" + serializer +
                ", value=" + value +
                ", changed=" + changed +
                ", name='" + name + '\'' +
                '}';
    }
}
