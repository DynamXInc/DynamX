package fr.dynamx.core.common.network.udp;

import io.netty.buffer.ByteBuf;

public abstract class UDPPacket {
    public abstract byte id();

    public abstract void write(ByteBuf var1);
}