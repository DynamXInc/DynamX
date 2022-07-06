package fr.dynamx.common.network.udp.auth;

import fr.dynamx.common.network.udp.UDPPacket;
import io.netty.buffer.ByteBuf;

public class UDPServerAuthenticationCompletePacket extends UDPPacket
{
    public byte id()
    {
        return (byte) 0;
    }

    public void write(ByteBuf out) {}
}