package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageUpdateChunk implements IDnxPacket, IMessageHandler<MessageUpdateChunk, IMessage> {
    private VerticalChunkPos[] chunksToUpdate;

    public MessageUpdateChunk() {
    }

    public MessageUpdateChunk(VerticalChunkPos[] chunksToUpdate) {
        this.chunksToUpdate = chunksToUpdate;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int controlSum = chunksToUpdate.length;
        buf.writeInt(controlSum);
        for (VerticalChunkPos pos : chunksToUpdate) {
            buf.writeInt(pos.x);
            buf.writeInt(pos.y);
            buf.writeInt(pos.z);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        chunksToUpdate = new VerticalChunkPos[size];
        for (int i = 0; i < size; i++) {
            chunksToUpdate[i] = new VerticalChunkPos(buf.readInt(),
                    buf.readInt(),
                    buf.readInt());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageUpdateChunk message, MessageContext ctx) {
        message.handleUDPReceive(Minecraft.getMinecraft().player, Side.CLIENT);
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void handleUDPReceive(EntityPlayer context, Side side) {
        if (context == null)
            return; // World is unloaded
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(context.world);
        if (physicsWorld != null && DynamXMain.proxy.shouldUseBulletSimulation(context.world)) {
            physicsWorld.schedule(() -> {
                Vector3fPool.openPool();
                for (VerticalChunkPos pos : chunksToUpdate) {
                    if (DynamXConfig.enableDebugTerrainManager) {
                        ChunkLoadingTicket ticket = physicsWorld.getTerrainManager().getTicket(pos);
                        if (ticket != null)
                            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.CHK_UPDATE, ChunkGraph.ActionLocation.UNKNOWN, ticket.getCollisions(), "Chunk changed from handleUDPReceive. Ticket " + ticket);
                    }
                    physicsWorld.getTerrainManager().onChunkChanged(pos);
                }
                Vector3fPool.closePool();
            });
        } else if (DynamXConfig.enableDebugTerrainManager)
            DynamXMain.log.info("RCV FAILZ " + physicsWorld + " " + DynamXMain.proxy.shouldUseBulletSimulation(context.world));
    }
}
