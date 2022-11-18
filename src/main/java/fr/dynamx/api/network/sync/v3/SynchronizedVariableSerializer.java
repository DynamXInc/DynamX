package fr.dynamx.api.network.sync.v3;

import io.netty.buffer.ByteBuf;

public interface SynchronizedVariableSerializer<T>
{
    void writeObject(ByteBuf buffer, T object);
    T readObject(ByteBuf buffer);
}
