package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsSimulationMode;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.physics.world.PhysicsSimulationModes;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXLoadingTasks;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSyncConfig implements IDnxPacket, IMessageHandler<MessageSyncConfig, IMessage> {
    private boolean reloadResources;
    private int syncDelay;
    private Map<Block, float[]> blockInfos;
    private List<Block> slopes;
    private int slopesLength;
    private boolean slopesPlace;
    private int physicsSimulationMode;
    private int entityId;

    public MessageSyncConfig() {
    }

    public MessageSyncConfig(boolean reloadResources, int syncDelay, Map<Block, float[]> blockInfos, List<Block> slopes, int slopesLength, boolean slopesPlace, IPhysicsSimulationMode physicsSimulationMode, int entityId) {
        this.reloadResources = reloadResources;
        this.syncDelay = syncDelay;
        this.blockInfos = blockInfos;
        this.slopes = slopes;
        this.slopesLength = slopesLength;
        this.slopesPlace = slopesPlace;
        this.physicsSimulationMode = physicsSimulationMode instanceof PhysicsSimulationModes.LightPhysics ? 1 : 0;
        this.entityId = entityId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(slopesPlace);
        buf.writeInt(slopesLength);
        buf.writeBoolean(reloadResources);
        buf.writeInt(syncDelay);
        buf.writeInt(blockInfos.size());
        blockInfos.forEach((b, f) -> {
            buf.writeInt(Block.getIdFromBlock(b));
            for (float f1 : f)
                buf.writeFloat(f1);
        });
        buf.writeInt(slopes.size());
        slopes.forEach(b -> buf.writeInt(Block.getIdFromBlock(b)));
        buf.writeInt(physicsSimulationMode);
        buf.writeInt(entityId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slopesPlace = buf.readBoolean();
        slopesLength = buf.readInt();
        reloadResources = buf.readBoolean();
        syncDelay = buf.readInt();
        blockInfos = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            Block b = Block.getBlockById(buf.readInt());
            float[] d = new float[]{buf.readFloat(), buf.readFloat()};
            if (b != null) blockInfos.put(b, d);
        }
        slopes = new ArrayList<>();
        size = buf.readInt();
        for (int i = 0; i < size; i++) {
            Block b = Block.getBlockById(buf.readInt());
            if (b != null) slopes.add(b);
        }
        physicsSimulationMode = buf.readInt();
        entityId = buf.readInt();
    }

    @Override
    public IMessage onMessage(MessageSyncConfig message, MessageContext ctx) {
        message.handleUDPReceive(null, Side.CLIENT);
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleUDPReceive(EntityPlayer context, Side side) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            DynamXConfig.mountedVehiclesSyncTickRate = syncDelay;
            if (reloadResources)
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK);
            ContentPackLoader.getBlocksGrip().clear();
            ContentPackLoader.getBlocksGrip().putAll(blockInfos);
            ContentPackLoader.slopes.clear();
            ContentPackLoader.slopes.addAll(slopes);
            ContentPackLoader.SLOPES_LENGTH = slopesLength;
            ContentPackLoader.PLACE_SLOPES = slopesPlace;
            DynamXContext.setPhysicsSimulationMode(Side.SERVER, physicsSimulationMode == 1 ? new PhysicsSimulationModes.LightPhysics() : new PhysicsSimulationModes.FullPhysics());
            // Fix issue with bungeecord not sending the new entityId to the client
            if (entityId != -1 && ClientEventHandler.MC.player != null) {
                ClientEventHandler.MC.player.setEntityId(entityId);
            }
        });
    }
}
