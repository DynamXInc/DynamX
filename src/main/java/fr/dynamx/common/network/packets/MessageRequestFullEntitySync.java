package fr.dynamx.common.network.packets;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import static fr.dynamx.common.DynamXMain.log;

public class MessageRequestFullEntitySync extends PhysicsEntityMessage<MessageRequestFullEntitySync> {
    public MessageRequestFullEntitySync() {
        super(null);
    }

    public MessageRequestFullEntitySync(PhysicsEntity<?> entity) {
        super(entity);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        throw new IllegalStateException();
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        EntityPlayerMP target = (EntityPlayerMP) player;
        log.info("Sending sync data to " + player + " ! Of: " + entity);
        if (target.connection != null && target.connection.getNetworkManager().isChannelOpen()) {
           //todo sync DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new MessagePhysicsEntitySync(entity, ServerPhysicsSyncManager.getTime(target), entity.getNetwork().getOutputSyncVars(), false), EnumPacketTarget.PLAYER, target);
            if (entity instanceof IModuleContainer.ISeatsContainer) {
                System.out.println("Forcing seats sync !");
                DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.PLAYER, target);
            }
            if (entity.getJointsHandler() != null) {
                entity.getJointsHandler().sync(target);
            }
        } else {
            DynamXMain.log.warn("Skipping resync item of " + entity + " for " + target + " : player not connected");
        }
    }
}