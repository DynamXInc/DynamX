package fr.dynamx.api.network.sync.v3;

import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;

public class SynchronizedEntityVariableSnapshot<T> {
    private final SynchronizedVariableSerializer<T> serializer;
    private T value;
    private boolean updated;

    public SynchronizedEntityVariableSnapshot(SynchronizedVariableSerializer<T> serializer, T initialValue) {
        this.value = initialValue;
        this.serializer = serializer;
    }

    public T get() {
        return value;
    }

    public void updateVariable(SynchronizedEntityVariable<T> variable) {
        if(updated) {
            variable.receiveValue(value);
            updated = false;
            if(value instanceof PooledHashMap) //TODO CLEAN
                ((PooledHashMap<?, ?>) value).release();
        }
    }

    public void read(ByteBuf buf) {
        value = serializer.readObject(buf);
        updated = true;
    }
}
