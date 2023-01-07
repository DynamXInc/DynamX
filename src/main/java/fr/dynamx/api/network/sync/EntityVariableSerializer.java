package fr.dynamx.api.network.sync;

import io.netty.buffer.ByteBuf;

public interface EntityVariableSerializer<T>
{
    void writeObject(ByteBuf buffer, T object);
    T readObject(ByteBuf buffer);
}
