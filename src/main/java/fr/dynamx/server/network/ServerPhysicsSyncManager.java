package fr.dynamx.server.network;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.vars.EntityPhysicsState;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashMap;
import java.util.Map;

/**
 * Hold player physics synchronization
 */
public class ServerPhysicsSyncManager {
    /**
     * Holds one {@link PlayerSyncBuffer} per connected player
     */
    private static final Map<EntityPlayer, PlayerSyncBuffer> sendBuffers = new HashMap<>();

    /**
     * Updates player buffers, sending all sync packets
     */
    public static void tick(Profiler profiler) {
        profiler.start(Profiler.Profiles.SYNC_BUFFER_UPDATE);
        sendBuffers.values().forEach(PlayerSyncBuffer::update);
        profiler.end(Profiler.Profiles.SYNC_BUFFER_UPDATE);
    }

    public static String toDebugString() {
        return sendBuffers.values().toString();
    }

    /**
     * Sets a player simulation time, used for driving sync, see {@link EntityPhysicsState}
     */
    public static void putTime(EntityPlayer player, int time) {
        if (sendBuffers.containsKey(player))
            sendBuffers.get(player).setSyncTime(time);
    }

    /**
     * Gets a player simulation time, used for driving sync, see {@link EntityPhysicsState}
     */
    public static int getTime(EntityPlayer player) {
        if (sendBuffers.containsKey(player))
            return sendBuffers.get(player).getSyncTime();
        return 0;
    }

    /**
     * Called on player disconnection to destroy its buffer
     */
    public static void onDisconnect(EntityPlayer player) {
        if (sendBuffers.containsKey(player))
            sendBuffers.remove(player).clear();
    }

    /**
     * Appends the data of this entity to the {@link PlayerSyncBuffer} of this player
     *
     * @param target     The target player
     * @param entity     The entity to sync
     * @param varsToSync The data to send
     */
    public static <T extends PhysicsEntity<?>> void addEntitySync(EntityPlayer target, T entity, PooledHashMap<Integer, EntityVariable<?>> varsToSync) {
        if (!sendBuffers.containsKey(target))
            sendBuffers.put(target, new PlayerSyncBuffer((EntityPlayerMP) target));
        sendBuffers.get(target).addEntitySync(entity, varsToSync);
    }
}
