package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;

public class SynchronizedEntityVariableSnapshot<T> {
    private final EntityVariableSerializer<T> serializer;
    private T value;
    private boolean updated;

    public SynchronizedEntityVariableSnapshot(EntityVariableSerializer<T> serializer, T initialValue) {
        this.value = initialValue;
        this.serializer = serializer;
    }

    public T get() {
        return value;
    }

    public void updateVariable(EntityVariable<T> variable) {
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
