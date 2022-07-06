package fr.dynamx.server.network.udp;

import net.minecraft.entity.player.EntityPlayerMP;

import java.math.BigInteger;
import java.net.InetSocketAddress;

public class UDPClient
{
    public EntityPlayerMP player;
    public InetSocketAddress socketAddress;
    private final int key;

    UDPClient(EntityPlayerMP player, InetSocketAddress socketAddress, String hash)
    {
        this.player = player;
        this.socketAddress = socketAddress;
        this.key = (int)(new BigInteger(hash.replaceAll("[^0-9.]", ""))).longValue();
    }

    public String toString()
    {
        return "Client[" + this.socketAddress + ": " + this.key + ", " + this.player + "]";
    }
}