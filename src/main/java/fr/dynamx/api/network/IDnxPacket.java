package fr.dynamx.api.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

public interface IDnxPacket extends IMessage {
    EnumNetworkType getPreferredNetwork();

    default void handleUDPReceive(EntityPlayer context, Side side)
    {
        throw new UnsupportedOperationException("UDP handling of this packet is not implemented !");
    }
}
