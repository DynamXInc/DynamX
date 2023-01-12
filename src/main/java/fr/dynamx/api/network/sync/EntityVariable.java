package fr.dynamx.api.network.sync;

import fr.dynamx.utils.debug.SyncTracker;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.function.BiConsumer;

public class EntityVariable<T> {
    @Getter
    private final BiConsumer<EntityVariable<T>, T> receiveCallback;
    @Getter
    private final SynchronizationRules synchronizationRule;
    @Getter
    private EntityVariableSerializer<T> serializer;
    private T value;
    protected boolean changed = true; //first sync
    @Getter
    private String name = "not_loaded";

    public EntityVariable(SynchronizationRules synchronizationRule, T initialValue) {
        this(null, synchronizationRule, initialValue);
    }

    public EntityVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule) {
        this(receiveCallback, synchronizationRule, null);
    }

    public EntityVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, T initialValue) {
        this.receiveCallback = receiveCallback;
        this.synchronizationRule = synchronizationRule;
        this.value = initialValue;
    }

    protected void init(String name, EntityVariableSerializer<?> type) {
        this.name = name;
        this.serializer = (EntityVariableSerializer<T>) type;
        //System.out.println("INIT " + name+" with " + type);
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        // todo wtf here
        if((value instanceof Float && SyncTracker.different((Float) value, (Float) this.value)) || (!(value instanceof Float) && value != this.value)) {
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
        if(value instanceof Map) { //TODO PUT IN SEPARATE CLASS
            ((Map)this.value).clear();
            ((Map)this.value).putAll((Map) value);
        } else
            this.value = value;
        setChanged(true); //server will send changes to clients
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
