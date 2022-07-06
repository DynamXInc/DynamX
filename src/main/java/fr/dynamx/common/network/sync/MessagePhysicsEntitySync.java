package fr.dynamx.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.api.network.sync.SynchronizedVariablesRegistry;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

/**
 * BulletEntity sync packet
 *
 * @see SynchronizedVariablesRegistry
 * @see fr.dynamx.api.network.sync.PhysicsEntityNetHandler
 */
public class MessagePhysicsEntitySync<T extends PhysicsEntity<?>> extends PhysicsEntityMessage<MessagePhysicsEntitySync<T>> {
    /**
     * The packet data
     */
    public Map<Integer, SynchronizedVariable<T>> varsToSync;
    /**
     * The "date" of the data contained in this packet
     */
    private int simulationTimeClient;

    private boolean doSizeTrack = false;
    private SyncType syncType;
    private T targetEntity;

    public MessagePhysicsEntitySync() {
        super(null);
    }

    public MessagePhysicsEntitySync(T entity, int simulationTimeClient, Map<Integer, SynchronizedVariable<T>> varsToSync, SyncType syncType) {
        super(entity);
        this.targetEntity = entity;
        this.varsToSync = varsToSync;
        this.simulationTimeClient = simulationTimeClient;
        this.syncType = syncType;
        //System.out.println("SEND "+varsToSync+" "+entityId);
        //System.out.println("Send "+simulationTimeClient);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        //System.out.println("Sending "+simulationTimeClient);
        int index = buf.writerIndex();
        super.toBytes(buf);
        buf.writeInt(simulationTimeClient);
        buf.writeInt(varsToSync.size());
        //buf.writeBoolean(doSizeTrack);
        final int[] j = {0};
        boolean[] log = {doSizeTrack};
        int size = buf.writerIndex();
        for (Map.Entry<Integer, SynchronizedVariable<T>> entry : varsToSync.entrySet()) {
            Integer i = entry.getKey();
            SynchronizedVariable<T> v = entry.getValue();
            if (log[0])
                System.out.println("Write var " + v.getClass() + " at " + j[0] + " /" + i + " " + entityId);
            buf.writeInt(i);
            switch (syncType) {
                case UDP_SYNC:
                    v.write(buf, false);
                    break;
                case UDP_COMPRESSED_SYNC:
                    v.write(buf, true);
                    break;
                case TCP_RESYNC:
                    v.writeEntityValues(targetEntity, buf);
                    break;
            }
            if (doSizeTrack) {
                size = buf.writerIndex() - size;
                buf.writeInt(size);
                size = buf.writerIndex();
            }
            j[0]++;
        }
        if (varsToSync instanceof PooledHashMap) {
            ((PooledHashMap<Integer, SynchronizedVariable<T>>) varsToSync).release();
        }

        ByteBuf f = buf.duplicate();
        f.resetWriterIndex();
        f.resetReaderIndex();
        if (doSizeTrack) {
            f.readerIndex(index);
            fromBytes(f);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        simulationTimeClient = buf.readInt();
        varsToSync = HashMapPool.get();
        int size = buf.readInt();
        //boolean doSizeTrack = buf.readBoolean();
        //System.out.println("Size tracking "+doSizeTrack);
        final int[] j = {0};
        boolean[] log = {doSizeTrack};
        int sized = buf.readerIndex();
        for (int i = 0; i < size; i++) {
            //if(log[0])
            //  System.out.println("Read var at "+j[0]+" "+entityId);
            SynchronizedVariable<T> v = null;
            try {
                int id = buf.readInt();
                v = (SynchronizedVariable<T>) SynchronizedVariablesRegistry.instantiate(id);
                if (log[0])
                    System.out.println("Read var at " + j[0] + " " + entityId + " " + v);
                v.read(buf);
                varsToSync.put(id, v);
                if (doSizeTrack) {
                    sized = buf.readerIndex() - sized;
                    int rd = buf.readInt();
                    if (sized != rd)
                        System.err.println("INDEX MISMATCH " + rd + " " + sized);
                    sized = buf.readerIndex();
                }
                j[0]++;
            } catch (Exception e) {
                throw new RuntimeException("Error reading sync packet for " + entityId + " has read " + varsToSync + " reading " + j[0] + " out of " + size + ". Var is " + v, e);
            }
        }
        //System.out.println("Rcv "+simulationTimeClient);
    }

    @Override
    public int getMessageId() {
        return 1;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    /**
     * @return The "date" of the data contained in this packet
     */
    public int getSimulationTimeClient() {
        return simulationTimeClient;
    }

    @Override
    public String toString() {
        return "MessagePhysicsEntitySync{" +
                "varsToSync=" + varsToSync +
                ", simulationTimeClient=" + simulationTimeClient +
                ", entityId=" + entityId +
                '}';
    }

    /* Old debug
    private EntityPlayer sender;
    public EntityPlayer getSender() {
        return sender;
    } */

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        //sender = context;
        super.handleUDPReceive(context, side);
    }

    /**
     * This is here thanks to java weird generic types
     */
    public static class Handler implements IMessageHandler<MessagePhysicsEntitySync, IMessage> {
        @Override
        public IMessage onMessage(MessagePhysicsEntitySync message, MessageContext ctx) {
            //if(ctx.side.isServer())
            //    message.sender = ctx.getServerHandler().player;
            message.onMessage(message, ctx);
            return null;
        }
    }

    public enum SyncType {
        UDP_SYNC, UDP_COMPRESSED_SYNC, TCP_RESYNC
    }
}