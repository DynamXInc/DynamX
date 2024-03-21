package fr.dynamx.common.network.packets;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageEntityInteract implements IDnxPacket, IMessageHandler<MessageEntityInteract, IMessage> {
    private int vehicleID;

    public MessageEntityInteract() {
    }

    public MessageEntityInteract(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
    }

    @Override
    public IMessage onMessage(MessageEntityInteract message, MessageContext ctx) {
        ctx.getServerHandler().player.server.addScheduledTask(() -> {
            message.handleUDPReceive(ctx.getServerHandler().player, Side.SERVER);
        });
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) context.world.getEntityByID(vehicleID);
        if (physicsEntity == null || context.getDistance(physicsEntity) > 21) {
            return;
        }
        if (context.getHeldItemMainhand().getItem() instanceof ItemWrench) {
            ((ItemWrench) context.getHeldItemMainhand().getItem()).interact(context, physicsEntity);
        } else if (!(physicsEntity instanceof IModuleContainer.ISeatsContainer) || !((IModuleContainer.ISeatsContainer) physicsEntity).hasSeats() || !((IModuleContainer.ISeatsContainer) physicsEntity).getSeats().isEntitySitting(context)) {
            if (!(physicsEntity instanceof PackPhysicsEntity)) {
                return;
            }
            PackPhysicsEntity<?, ?> targetEntity = (PackPhysicsEntity<?, ?>) physicsEntity;
            //If we clicked a part, try to interact with it.
            Vector3fPool.openPool();
            InteractivePart hitPart = targetEntity.getHitPart(context);
            if (hitPart != null && hitPart.canInteract(targetEntity, context)) {
                if ((hitPart instanceof PartEntitySeat && ((PartEntitySeat) hitPart).hasDoor()) && context.isSneaking()) {
                    return;
                }
                if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, targetEntity, hitPart)))
                    hitPart.interact(targetEntity, context);
            } else
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, targetEntity, null));
            Vector3fPool.closePool();
        }
    }
}
