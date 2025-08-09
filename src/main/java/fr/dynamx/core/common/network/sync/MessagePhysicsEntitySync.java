package fr.dynamx.core.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.core.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.core.utils.optimization.HashMapPool;
import fr.dynamx.core.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private int simulationTimeClient;

    private final boolean doSizeTrack = false;
    private boolean lightData;
    private T targetEntity;

    public MessagePhysicsEntitySync() {
        super(null);
    }

    public MessagePhysicsEntitySync(T entity, int simulationTimeClient, Map<Integer, EntityVariable<?>> varsToSync, boolean lightData) {
        super(entity);
        this.targetEntity = entity;
        this.varsToSend = varsToSync;
        this.simulationTimeClient = simulationTimeClient;
        this.lightData = lightData;
        // System.out.println("SEND "+entity.ticksExisted+" "+entityId);
        // System.out.println("Send "+simulationTimeClient);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        //  System.out.println("Sending "+simulationTimeClient);
        int index = buf.writerIndex();
        super.toBytes(buf);
        buf.writeInt(simulationTimeClient);
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
        simulationTimeClient = buf.readInt();
        varsToRead = HashMapPool.get();
        int size = buf.readInt();
        //boolean doSizeTrack = buf.readBoolean();
        //System.out.println("Size tracking "+doSizeTrack);
        final int[] j = {0};
        boolean[] log = {doSizeTrack};
        int sized = buf.readerIndex();
        for (int i = 0; i < size; i++) {
            //if(log[0])
            //  System.out.println("Read var at "+j[0]+" "+entityId);
            SynchronizedEntityVariableSnapshot<?> v = null;
            int id = -1;
            try {
                id = buf.readInt();
                EntityVariableSerializer<?> serializer = SynchronizedEntityVariableRegistry.getSerializerMap().get(id);
                if (serializer == null)
                    throw new IllegalArgumentException("Serializer not found for id " + id + " in " + SynchronizedEntityVariableRegistry.getSerializerMap() + ". Variable is " + SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(id));
                v = new SynchronizedEntityVariableSnapshot<>(serializer, null);
                if (log[0])
                    System.out.println("Read var at " + j[0] + " " + entityId + " " + v);
                v.read(buf);
                varsToRead.put(id, v);
                if (doSizeTrack) {
                    sized = buf.readerIndex() - sized;
                    int rd = buf.readInt();
                    if (sized != rd)
                        System.err.println("INDEX MISMATCH " + rd + " " + sized);
                    sized = buf.readerIndex();
                }
                j[0]++;
            } catch (Exception e) {
                DynamXMain.log.error("[PRE-ERROR-DEBUG] Synchronized variable registry is " + SynchronizedEntityVariableRegistry.getSyncVarRegistry() + " and serializer map is " + SynchronizedEntityVariableRegistry.getSerializerMap());
                final List<String> readVars = varsToRead.entrySet().stream().map(entry ->
                        SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(entry.getKey()) + " (id: " + entry.getKey() + ") =" + entry.getValue()
                ).collect(Collectors.toList());
                throw new RuntimeException("Error reading sync packet for " + entityId + " has read " + readVars + " reading " + j[0] + " out of " + size
                        + ". Var snapshot is " + v + ". Reading variable name is " + SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(id) + " (id=" + id + ")", e);
            }
        }
        //System.out.println("Rcv "+simulationTimeClient);
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        //System.out.println("Rcv syncs " + entity.ticksExisted);
        ((MPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        ((MPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
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
                "varsToSend=" + varsToSend +
                ", varsToRead=" + varsToRead +
                ", simulationTimeClient=" + simulationTimeClient +
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