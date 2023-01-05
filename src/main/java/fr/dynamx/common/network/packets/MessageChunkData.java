package fr.dynamx.common.network.packets;

import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.physics.terrain.cache.RemoteTerrainCache;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class MessageChunkData implements IDnxPacket {
    private static final byte version = 3;

    private byte[] dataType;
    private VerticalChunkPos pos;
    private byte[] data;

    public MessageChunkData() {
    }

    public MessageChunkData(VerticalChunkPos pos, byte[] dataType, byte[] data) {
        this.pos = pos;
        this.dataType = dataType;
        this.data = data;
    }

    public MessageChunkData(VerticalChunkPos pos, byte[] dataType, List<ITerrainElement> terrainElements) {
        this.pos = pos;
        this.dataType = dataType;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(new GZIPOutputStream(data));
            out.writeInt(terrainElements.size());
            for (ITerrainElement e : terrainElements) {
                out.writeByte(e.getFactory().ordinal());
                e.save(ITerrainElement.TerrainSaveType.NETWORK, out);
            }
            out.close();
            this.data = data.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ouch", e);
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        byte ve = buf.readByte();
        if (ve != version) {
            throw new UnsupportedOperationException("Wrong encoding version, found " + ve + " and should be " + version);
        }
        pos = new VerticalChunkPos(buf.readInt(), buf.readInt(), buf.readInt());
        dataType = new byte[]{buf.readByte(), buf.readByte()};
        data = new byte[buf.readInt()];
        buf.readBytes(data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(version);
        buf.writeInt(pos.x);
        buf.writeInt(pos.y);
        buf.writeInt(pos.z);
        buf.writeByte(dataType[0]);
        buf.writeByte(dataType[1]);

        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    public static class Handler implements IMessageHandler<MessageChunkData, IDnxPacket> {
        private final ExecutorService POOL = Executors.newFixedThreadPool(2, new DynamXThreadedModLoader.DefaultThreadFactory("DnxCliCollsLoader"));

        @Override
        @SideOnly(Side.CLIENT)
        public IDnxPacket onMessage(MessageChunkData message, MessageContext ctx) {
            POOL.submit(() -> {
                Profiler profiler = Profiler.get();
                profiler.start(Profiler.Profiles.TERRAIN_LOADER_TICK);
                ((RemoteTerrainCache) DynamXContext.getPhysicsWorld(ClientEventHandler.MC.world).getTerrainManager().getCache()).receiveChunkData(message.pos, message.dataType[0], message.dataType[1], message.data);
                profiler.end(Profiler.Profiles.TERRAIN_LOADER_TICK);
                profiler.update();
                if (profiler.isActive()) //Profiling
                {
                    List<String> st = profiler.getData();
                    if (!st.isEmpty()) {
                        profiler.printData("Network terrain thread");
                        profiler.reset();
                    }
                }
            });
            return null;
        }
    }
}
