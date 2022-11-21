package fr.dynamx.common.network.packets;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

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
        log.info("[Full-Sync] Sending sync data to " + player + " ! Of: " + entity);
        if (target.connection != null && target.connection.getNetworkManager().isChannelOpen()) {
            entity.getSynchronizer().resyncEntity((EntityPlayerMP) player);
        } else {
            DynamXMain.log.warn("Skipping resync item of " + entity + " for " + target + " : player not connected");
        }
    }
}