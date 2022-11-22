package fr.dynamx.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collection;

public class MessageMultiPhysicsEntitySync implements IDnxPacket, IMessageHandler<MessageMultiPhysicsEntitySync, IMessage> {
    private Collection<MessagePhysicsEntitySync<?>> syncs;

    public MessageMultiPhysicsEntitySync() {
    }

    public MessageMultiPhysicsEntitySync(Collection<MessagePhysicsEntitySync<?>> syncs) {
        this.syncs = syncs;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        syncs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MessagePhysicsEntitySync<?> msg = new MessagePhysicsEntitySync<>();
            msg.fromBytes(buf);
            syncs.add(msg);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(syncs.size());
        syncs.forEach(s -> s.toBytes(buf));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageMultiPhysicsEntitySync message, MessageContext ctx) {
        message.handleUDPReceive(Minecraft.getMinecraft().player, Side.CLIENT);
        return null;
    }

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        //System.out.println("Rcv syncs " + context.ticksExisted);
        syncs.forEach(s -> s.handleUDPReceive(context, side));
    }

    @Override
    public String toString() {
        return "MessageMultiPhysicsEntitySync{" +
                "syncs=" + syncs +
                '}';
    }
}
