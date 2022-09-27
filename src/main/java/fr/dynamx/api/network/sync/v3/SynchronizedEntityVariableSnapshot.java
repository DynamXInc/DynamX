package fr.dynamx.api.network.sync.v3;

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
        }
    }

    public void read(ByteBuf buf) {
        value = serializer.readObject(buf, value);
        updated = true;
    }
}
