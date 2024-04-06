package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessagePing implements IDnxPacket, net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<MessagePing, IMessage> {
    private long sentTime;
    private boolean manual;

    public MessagePing() {
    }

    public MessagePing(long creationTime, boolean manual) {
        this.sentTime = creationTime;
        this.manual = manual;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(sentTime);
        buf.writeBoolean(manual);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sentTime = buf.readLong();
        manual = buf.readBoolean();
    }

    @Override
    public IMessage onMessage(MessagePing message, MessageContext ctx) {
        if (ctx.side.isServer())
            return new MessagePing(message.sentTime, message.manual);
        else {
            clientHandle(message);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void clientHandle(MessagePing message) {
        ClientPhysicsSyncManager.pingMs = ((int) (System.currentTimeMillis() - message.sentTime)) / 2;
        ClientPhysicsSyncManager.lastPing = message.sentTime;
        NetworkActivityTracker.addPing(ClientPhysicsSyncManager.pingMs);
        if (message.manual)
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("[DynamX] Your ping is " + ClientPhysicsSyncManager.pingMs + " ms"));
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        if (side.isServer())
            DynamXContext.getNetwork().sendToClient(new MessagePing(sentTime, manual), EnumPacketTarget.PLAYER, ((EntityPlayerMP) context));
        else {
            clientHandle(this);
        }
    }
}