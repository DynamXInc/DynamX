package fr.dynamx.core.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.contentpack.ContentPackLoader;
import fr.dynamx.core.common.physics.world.PhysicsSimulationModes;
import fr.dynamx.forge.DynamXConfig;
import fr.dynamx.core.utils.DynamXLoadingTasks;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
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
    private int physicsSimulationMode;
    private int entityId;
    private int serverSynchronizedVariablesCount;

    public MessageSyncConfig() {
    }

    public MessageSyncConfig(boolean reloadResources, int entityId) {
        this.reloadResources = reloadResources;
        this.syncDelay = DynamXConfig.mountedVehiclesSyncTickRate;
        this.physicsSimulationMode = DynamXContext.getPhysicsSimulationMode(Side.CLIENT) instanceof PhysicsSimulationModes.LightPhysics ? 1 : 0;
        this.entityId = entityId;
        this.serverSynchronizedVariablesCount = SynchronizedEntityVariableRegistry.getSyncVarRegistry().size();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(reloadResources);
        buf.writeInt(syncDelay);
        buf.writeInt(physicsSimulationMode);
        buf.writeInt(entityId);
        buf.writeInt(serverSynchronizedVariablesCount);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        reloadResources = buf.readBoolean();
        syncDelay = buf.readInt();
        physicsSimulationMode = buf.readInt();
        entityId = buf.readInt();
        serverSynchronizedVariablesCount = buf.readInt();
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
            if (reloadResources) {
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK);
            }
            DynamXContext.setPhysicsSimulationMode(Side.SERVER, physicsSimulationMode == 1 ? new PhysicsSimulationModes.LightPhysics() : new PhysicsSimulationModes.FullPhysics());
            // Fix issue with bungeecord not sending the new entityId to the client
            if (entityId != -1 && ClientEventHandler.MC.player != null) {
                ClientEventHandler.MC.player.setEntityId(entityId);
            }
            // Ensure the server and the client have the same synchronized variables, using a basic "count of variables" check
            // The DynamX network check handler already checks if the client and server has the same addon, but different versions of the same addon, or bugged addons, can give different synchronized variables
            if (SynchronizedEntityVariableRegistry.getSyncVarRegistry().size() != serverSynchronizedVariablesCount) {
                String message = "[DynamX] Server and client synchronized variables count mismatch! Verify you have the SAME DynamX addons on server AND client sides. Server: " + serverSynchronizedVariablesCount + " Client: " + SynchronizedEntityVariableRegistry.getSyncVarRegistry().size();
                Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(new TextComponentString(message));
                throw new IllegalStateException(message);
            }
        });
    }
}
