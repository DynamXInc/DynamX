package fr.dynamx.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

/**
 * BulletEntity sync packet
 *
 * @see SynchronizedVariablesRegistry
 * @see fr.dynamx.api.network.sync.PhysicsEntityNetHandler
 */
public class MessagePhysicsEntitySync<T extends PhysicsEntity<?>> extends PhysicsEntityMessage<MessagePhysicsEntitySync<T>> {
    //@Getter
    private Map<Integer, EntityVariable<?>> varsToSend;
    @Getter
    private PooledHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> varsToRead;
    /**
     * The "date" of the data contained in this packet
     */
    private int simulationTime;

    private final boolean doSizeTrack = false;
    private boolean lightData;
    private T targetEntity;
    @Getter
    private int messageSize;
    @Getter
    private Map<String, Integer> moduleSizes;

    public MessagePhysicsEntitySync() {
        super(null);
    }

    public MessagePhysicsEntitySync(T entity, int simulationTime, Map<Integer, EntityVariable<?>> varsToSync, boolean lightData) {
        super(entity);
        this.targetEntity = entity;
        this.varsToSend = varsToSync;
        this.simulationTime = simulationTime;
        this.lightData = lightData;
       // System.out.println("SEND "+entity.ticksExisted+" "+entityId);
       // System.out.println("Send "+simulationTimeClient);
    }

    @Override
    public void toBytes(ByteBuf buf) {
      //  System.out.println("Sending "+simulationTimeClient);
        int index = buf.writerIndex();
        super.toBytes(buf);
        buf.writeInt(simulationTime);
        buf.writeInt(varsToSend.size());
        //buf.writeBoolean(doSizeTrack);
        final int[] j = {0};
        boolean[] log = {doSizeTrack};
        int size = buf.writerIndex();
        for (Map.Entry<Integer, EntityVariable<?>> entry : varsToSend.entrySet()) {
            Integer i = entry.getKey();
            EntityVariable<Object> v = (EntityVariable<Object>) entry.getValue();
            if (log[0])
                System.out.println("Write var " + v.getClass() + " at " + j[0] + " /" + i + " " + entityId);
            buf.writeInt(i);
            v.writeValue(buf, lightData);
            v.setChanged(false);
            if (doSizeTrack) {
                size = buf.writerIndex() - size;
                buf.writeInt(size);
                size = buf.writerIndex();
            }
            j[0]++;
        }
        if (varsToSend instanceof PooledHashMap) {
            ((PooledHashMap<Integer, EntityVariable<?>>) varsToSend).release();
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
        simulationTime = buf.readInt();
        varsToRead = HashMapPool.get();
        int size = buf.readInt();
        //boolean doSizeTrack = buf.readBoolean();
        //System.out.println("Size tracking "+doSizeTrack);
        final int[] j = {0};
        boolean[] log = {doSizeTrack};
        int sized = buf.readerIndex();
        int size0 = sized;
        moduleSizes = new HashMap<>();
        for (int i = 0; i < size; i++) {
            //if(log[0])
            //  System.out.println("Read var at "+j[0]+" "+entityId);
            SynchronizedEntityVariableSnapshot<?> v = null;
            try {
                int id = buf.readInt();
                v = new SynchronizedEntityVariableSnapshot<>(SynchronizedEntityVariableRegistry.getSerializerMap().get(id), null);
                if (log[0])
                    System.out.println("Read var at " + j[0] + " " + entityId + " " + v);
                v.read(buf);
                varsToRead.put(id, v);
                sized = buf.readerIndex() - sized;
                if (doSizeTrack) {
                    int rd = buf.readInt();
                    if (sized != rd)
                        System.err.println("INDEX MISMATCH " + rd + " " + sized);
                }
                moduleSizes.put(SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(id), sized);
                sized = buf.readerIndex();
                j[0]++;
            } catch (Exception e) {
                throw new RuntimeException("Error reading sync packet for " + entityId + " has read " + varsToRead + " reading " + j[0] + " out of " + size + ". Var is " + v, e);
            }
        }
        //System.out.println("Rcv "+simulationTimeClient);
        messageSize = buf.readerIndex() - size0;
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        //System.out.println("Rcv syncs " + entity.ticksExisted);
        ((MPPhysicsEntitySynchronizer<?>)entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        ((MPPhysicsEntitySynchronizer<?>)entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    /**
     * @return The "date" of the data contained in this packet
     */
    public int getSimulationTime() {
        return simulationTime;
    }

    @Override
    public String toString() {
        return "MessagePhysicsEntitySync{" +
                "varsToSend=" + varsToSend +
                ", varsToRead=" + varsToRead +
                ", simulationTimeClient=" + simulationTime +
                ", doSizeTrack=" + doSizeTrack +
                ", lightData=" + lightData +
                ", targetEntity=" + targetEntity +
                '}';
    }

    /**
     * This is here thanks to java weird generic types
     */
    public static class Handler implements IMessageHandler<MessagePhysicsEntitySync, IMessage> {
        @Override
        public IMessage onMessage(MessagePhysicsEntitySync message, MessageContext ctx) {
            message.onMessage(message, ctx);
            return null;
        }
    }
}