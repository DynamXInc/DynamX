package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.cache.TerrainFile;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
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
        //if(DynamXConfig.enableDebugTerrainManager)
        TerrainFile.ULTIMATEDEBUG = false;
        //System.out.println("Receive dirty "+ Arrays.toString(chunksToUpdate) +" "+DynamXContext.getPhysicsWorld()+" "+DynamXMain.proxy.shouldUseBulletSimulation(context.world));
        if (DynamXContext.getPhysicsWorld() != null && DynamXMain.proxy.shouldUseBulletSimulation(context.world)) {
            DynamXContext.getPhysicsWorld().schedule(() -> {
                Vector3fPool.openPool();
                for (VerticalChunkPos pos : chunksToUpdate) {
                    DynamXContext.getPhysicsWorld().getTerrainManager().onChunkChanged(pos);
                }
                Vector3fPool.closePool();
            });
        } else if (DynamXConfig.enableDebugTerrainManager)
            System.out.println("RCV FAILZ " + DynamXContext.getPhysicsWorld() + " " + DynamXMain.proxy.shouldUseBulletSimulation(context.world));
    }
}
