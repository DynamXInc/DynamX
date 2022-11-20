package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import static fr.dynamx.common.DynamXMain.log;

public abstract class PhysicsEntityMessage<T extends PhysicsEntityMessage> implements IDnxPacket, IMessageHandler<T, IMessage> {
    /**
     * Handled internally, should not be modified
     */
    protected int entityId = -1;

    /**
     * Use the other constructor with entity set to null
     */
    private PhysicsEntityMessage() {
    }

    /*
     * @param entity can be null for empty constructor
     */
    public PhysicsEntityMessage(@Nullable PhysicsEntity<?> entity) {
        if (entity != null)
            this.entityId = entity.getEntityId();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public IMessage onMessage(PhysicsEntityMessage message, MessageContext ctx) {
        if(ctx.side.isClient()) {
            clientSchedule(() -> processMessage(ctx.side, message, getClientPlayer()));
        } else {
            DynamXContext.getPhysicsWorld().schedule(() -> processMessage(ctx.side, message, ctx.getServerHandler().player));
        }
        return null;
    }

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        processMessage(side, this, context);
    }

    protected void processMessage(Side side, PhysicsEntityMessage<?> message, EntityPlayer player) {
        if (message.entityId == -1)
            throw new IllegalArgumentException("EntityId isn't valid, maybe you don't call fromBytes and toBytes " + message);
        Entity ent = player.world.getEntityByID(message.entityId);
        if (ent instanceof PhysicsEntity) {
            if (side.isClient()) {
                processMessageClient(message, (PhysicsEntity<?>) ent, player);
            } else {
                processMessageServer(message, (PhysicsEntity<?>) ent, player);
            }
        } else if (message instanceof MessageSeatsSync || DynamXConfig.enableDebugTerrainManager) {
            log.warn("PhysicsEntity with id " + message.entityId + " not found for message with type " + message + " sent from " + player);
        }
    }

    @SideOnly(Side.CLIENT)
    protected abstract void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player);
    protected abstract void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player);

    @SideOnly(Side.CLIENT)
    protected EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @SideOnly(Side.CLIENT)
    protected void clientSchedule(Runnable task) {
        if (getPreferredNetwork() != EnumNetworkType.VANILLA_TCP && DynamXContext.getPhysicsWorld() != null) { //If initialized, and not a "vanilla packet" (vanilla packet does not always concern physics, like seats)
            DynamXContext.getPhysicsWorld().schedule(task);
        } else {
            Minecraft.getMinecraft().addScheduledTask(task);
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
