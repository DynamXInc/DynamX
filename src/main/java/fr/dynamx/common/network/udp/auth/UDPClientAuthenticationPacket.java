package fr.dynamx.common.network.udp.auth;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.UDPPacket;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class UDPClientAuthenticationPacket extends UDPPacket {
    String hash;

    public UDPClientAuthenticationPacket(String hash) {
        this.hash = hash;
    }

    public byte id() {
        return (byte) 0;
    }

    public void write(ByteBuf out) {
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Writing auth RQ !");
        ByteBufUtils.writeUTF8String(out, hash);
    }
}