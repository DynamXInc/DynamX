package fr.dynamx.server.network;

import com.google.common.collect.Queues;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessageMultiPhysicsEntitySync;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.vars.EntityPhysicsState;
import fr.dynamx.server.command.CmdNetworkConfig;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.*;

/**
 * Manages sending of sync packets for a player, buffers and merges packets with lowest priority (distant entities) to limit band-width, while keeping a good sync <br>
 * This doc names "packet" the data of an entity, but actually only one packet is sent on the network, containing all data for one tick
 *
 * @author aym
 */
public class PlayerSyncBuffer {
    /**
     * Limit of packets sent for one tick, except delayed ones, modifiable via /dynamx command
     */
    public static int NEW_SENDS_LIMIT = 20;
    /**
     * Limit of delayed packets sent for one tick, modifiable via /dynamx command
     */
    public static int DELAYED_SENDS_LIMIT = 10;
    /**
     * Max radius of packets that are always sent, modifiable via /dynamx command
     */
    public static int FIRST_RADIUS = 21 * 21;
    /**
     * Max radius of delayed packets that are always sent on the second attempt, modifiable via /dynamx command
     */
    public static int SECOND_RADIUS = 39 * 39;
    /**
     * Max number of delaying for a packet, modifiable via /dynamx command
     */
    public static int MAX_SKIP = 4;
    /**
     * Number of entities synced by one {@link MessageMultiPhysicsEntitySync}, bigger number implies less packets, but heavier packet that my bigger than the size limit
     */
    public static int ENTITIES_PER_PACKETS = 10;

    /**
     * Managed player
     */
    private final EntityPlayerMP playerIn;
    /**
     * Packets added this tick
     */
    private final Queue<SyncItem<?>> queuedPackets = Queues.newArrayDeque();
    /**
     * Packets delayed the last ticks
     */
    private final List<SyncItem<?>> delayedPackets = new ArrayList<>();
    /**
     * Sync time used for driving synchronisation, see {@link EntityPhysicsState}
     */
    private int syncTime;

    public PlayerSyncBuffer(EntityPlayerMP playerIn) {
        this.playerIn = playerIn;
    }

    /**
     * Adds entity data to send, merging with eventual previously delayed packets
     *
     * @param entity     The entity to sync
     * @param varsToSync Its data
     */
    public <T extends PhysicsEntity<?>> void addEntitySync(T entity, PooledHashMap<Integer, SynchronizedVariable<T>> varsToSync) {
        SyncItem<T> sync = new SyncItem<>(entity, varsToSync);
        if (delayedPackets.contains(sync)) {
            for (SyncItem<?> s : delayedPackets) {
                if (s.equals(sync)) {
                    sync.merge((SyncItem<T>) s);
                    break;
                }
            }
            delayedPackets.remove(sync);
        }
        varsToSync.forEach((i, v) -> {
            v.validate(entity, 3);
        });
        queuedPackets.add(sync);
    }

    /**
     * Updates buffers, and send data that need to be sent
     */
    public void update() {
        /*if(DynamXCommands.sync_buff)
        {
          //  System.out.println("AT");
           System.out.println(this.toString());
        }*/
        final Queue<MessagePhysicsEntitySync<?>> sendQueue = new ArrayDeque<>();
        if (queuedPackets.size() <= NEW_SENDS_LIMIT && delayedPackets.size() <= DELAYED_SENDS_LIMIT) {
            if (!delayedPackets.isEmpty()) {
                delayedPackets.forEach(syncItem -> syncItem.send(sendQueue));
                delayedPackets.clear();
            }
            queuedPackets.forEach(syncItem -> syncItem.send(sendQueue));
            queuedPackets.clear();
        } else //too much to send
        {
            List<SyncItem<?>> keep = new ArrayList<>();
            if (!delayedPackets.isEmpty()) {
                delayedPackets.forEach(s -> {
                    if (s.entity.getDistanceSq(playerIn) > SECOND_RADIUS && s.skip())
                        keep.add(s);
                    else {
                        s.send(sendQueue);
                    }
                });
                delayedPackets.clear();
            }
            queuedPackets.forEach(s -> {
                if (s.entity.getDistanceSq(playerIn) > FIRST_RADIUS && s.skip())
                    keep.add(s);
                else {
                    s.send(sendQueue);
                }
            });
            queuedPackets.clear();

            //System.out.println("FC "+count[0]);
            while (!keep.isEmpty() && sendQueue.size() <= NEW_SENDS_LIMIT + DELAYED_SENDS_LIMIT) {
                SyncItem<?> s = keep.remove(0);
                s.send(sendQueue);
            }
            //System.out.println("FA "+count[0]);
            if (!keep.isEmpty()) {
                delayedPackets.addAll(keep);
                //System.out.println("Delay "+delayedPackets+" "+keep);
                keep.clear();
            }
        }
        syncTime++;
        /*if(DynamXCommands.sync_buff && !delayedPackets.isEmpty())
        {
            System.out.println("PT");
            System.out.println(this.toString()+" "+sendQueue.size());
        }*/
        if (!sendQueue.isEmpty()) {
            if (CmdNetworkConfig.sync_buff && playerIn.getName().equalsIgnoreCase("aymericred"))
                System.out.println("Send you " + sendQueue);
            if (sendQueue.size() == 1)
                DynamXContext.getNetwork().sendToClient(sendQueue.poll(), EnumPacketTarget.PLAYER, playerIn);
            else {
                while (sendQueue.size() > ENTITIES_PER_PACKETS) {
                    List<MessagePhysicsEntitySync<?>> buff = new ArrayList<>();
                    for (int i = 0; i < ENTITIES_PER_PACKETS; i++) {
                        buff.add(sendQueue.poll());
                    }
                    DynamXContext.getNetwork().sendToClient(new MessageMultiPhysicsEntitySync(buff), EnumPacketTarget.PLAYER, playerIn);
                }
                DynamXContext.getNetwork().sendToClient(new MessageMultiPhysicsEntitySync(sendQueue), EnumPacketTarget.PLAYER, playerIn);
            }
        }
    }

    /**
     * Sets sync time used for driving synchronisation, see {@link EntityPhysicsState}
     */
    public void setSyncTime(int syncTime) {
        this.syncTime = syncTime;
    }

    /**
     * Gets sync time used for driving synchronisation, see {@link EntityPhysicsState}
     */
    public int getSyncTime() {
        return syncTime;
    }

    /**
     * Clears buffers of entity data
     */
    public void clear() {
        queuedPackets.clear();
        delayedPackets.clear();
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "player=" + playerIn.getName() +
                ", queued=" + queuedPackets.size() +
                ", delayed=" + delayedPackets.size() +
                ", syncT=" + syncTime +
                '}';
    }

    /**
     * Handles data of an entity, and number of delaying of its sync
     */
    private class SyncItem<T extends PhysicsEntity<?>> {
        private final T entity;
        private final PooledHashMap<Integer, SynchronizedVariable<T>> varsToSync;
        private int skippedSends;

        private SyncItem(T entity, PooledHashMap<Integer, SynchronizedVariable<T>> varsToSync) {
            this.entity = entity;
            this.varsToSync = varsToSync;
        }

        /**
         * Adds a {@link MessagePhysicsEntitySync} to the send queue, if this entity is not dead
         */
        private void send(Queue<MessagePhysicsEntitySync<?>> sendQueue) {
            varsToSync.forEach((i, t) -> t.validate(entity, 4));
            if (!entity.isDead) {
                sendQueue.add(new MessagePhysicsEntitySync(entity, syncTime, varsToSync, varsToSync.size() > NEW_SENDS_LIMIT ? MessagePhysicsEntitySync.SyncType.UDP_COMPRESSED_SYNC : MessagePhysicsEntitySync.SyncType.UDP_SYNC));
            }
        }

        /**
         * Counts one delay for this packet
         *
         * @return False is number of delaying has reached the maximum
         */
        private boolean skip() {
            if (skippedSends < MAX_SKIP)
                skippedSends++;
            return skippedSends < MAX_SKIP;
        }

        /**
         * Adds variables of withOlder only if this don't have these variables already stored, also updates number of delaying
         *
         * @param withOlder Older data to retrieve if we don't have it
         */
        private void merge(SyncItem<T> withOlder) {
            withOlder.varsToSync.forEach((i, s) -> {
                if (!varsToSync.containsKey(i))
                    varsToSync.put(i, s);
            });
            withOlder.varsToSync.release();
            skippedSends += withOlder.skippedSends;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SyncItem<?> syncItem = (SyncItem<?>) o;
            return entity.equals(syncItem.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity);
        }

        @Override
        public String toString() {
            return "{" +
                    "e=" + entity.getEntityId() +
                    ", c=" + varsToSync.size() +
                    ", sk=" + skippedSends +
                    '}';
        }
    }
}
