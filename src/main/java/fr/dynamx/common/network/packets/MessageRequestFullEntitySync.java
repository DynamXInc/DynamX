package fr.dynamx.common.network.packets;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import static fr.dynamx.common.DynamXMain.log;

public class MessageRequestFullEntitySync extends PhysicsEntityMessage<MessageRequestFullEntitySync>
{
    public MessageRequestFullEntitySync() {super(null);}

    public MessageRequestFullEntitySync(PhysicsEntity<?> entity) {
        super(entity);
    }

    @Override
    public int getMessageId() {
        return 42;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public IMessage onMessage(PhysicsEntityMessage message, MessageContext ctx) {
        if (message.entityId == -1)
            throw new IllegalArgumentException("EntityId isn't valid, maybe you don't call fromBytes and toBytes " + message);
        processMessage(message, ctx.getServerHandler().player);
        return null;
    }

    @Override
    protected void processMessage(PhysicsEntityMessage<?> message, EntityPlayer player) {
        EntityPlayerMP target = (EntityPlayerMP) player;
        Entity ent = player.world.getEntityByID(message.entityId);
        log.info("Sending sync data to "+player+" ! Of: "+ent);
        if (ent instanceof PhysicsEntity) {
            PhysicsEntity<?> entity = (PhysicsEntity<?>) ent;
            if (target.connection != null && target.connection.getNetworkManager().isChannelOpen()) {
                DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new MessagePhysicsEntitySync(entity, ServerPhysicsSyncManager.getTime(target), entity.getNetwork().getOutputSyncVars(), MessagePhysicsEntitySync.SyncType.TCP_RESYNC), EnumPacketTarget.PLAYER, target);
                if (entity instanceof IModuleContainer.ISeatsContainer) {
                    System.out.println("Forcing seats sync !");
                    DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.PLAYER, target);
                }
                if (entity.getJointsHandler() != null) {
                    entity.getJointsHandler().sync(target);
                }
            } else {
                DynamXMain.log.warn("Skipping resync item of "+entity+" for "+target+" : player not connected");
            }
        } else if (message instanceof MessageSeatsSync || DynamXConfig.enableDebugTerrainManager) {
            log.warn("PhysicsEntity with id " + message.entityId + " not found for message with type " + message.getMessageId() + " sent from " + player);
        }
    }
}