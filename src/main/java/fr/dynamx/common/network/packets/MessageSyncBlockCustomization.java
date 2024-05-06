package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSyncBlockCustomization implements IDnxPacket, IMessageHandler<MessageSyncBlockCustomization, IDnxPacket> {

    private BlockPos blockPos;
    private Vector3f relativeTranslation, relativeScale, relativeRotation;

    public MessageSyncBlockCustomization() {
    }

    public MessageSyncBlockCustomization(BlockPos blockPos, Vector3f relativeTranslation, Vector3f relativeScale, Vector3f relativeRotation) {
        this.blockPos = blockPos;
        this.relativeTranslation = relativeTranslation;
        this.relativeScale = relativeScale;
        this.relativeRotation = relativeRotation;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeBlockPos(buf, blockPos);
        DynamXUtils.writeVector3f(buf, relativeTranslation);
        DynamXUtils.writeVector3f(buf, relativeScale);
        DynamXUtils.writeVector3f(buf, relativeRotation);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = DynamXUtils.readBlockPos(buf);
        relativeTranslation = DynamXUtils.readVector3f(buf);
        relativeScale = DynamXUtils.readVector3f(buf);
        relativeRotation = DynamXUtils.readVector3f(buf);
    }

    @Override
    public IDnxPacket onMessage(MessageSyncBlockCustomization message, MessageContext ctx) {
        ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
            World world = ctx.getServerHandler().player.world;
            TEDynamXBlock te = (TEDynamXBlock) ctx.getServerHandler().player.world.getTileEntity(message.blockPos);
            if (te != null) {
                if (ctx.getServerHandler().player.canUseCommand(4, "dynamx block_customization")) {
                    te.setRelativeTranslation(message.relativeTranslation);
                    te.setRelativeScale(message.relativeScale);
                    te.setRelativeRotation(message.relativeRotation);
                    te.markDirty();
                    te.markCollisionsDirty();
                    world.markBlockRangeForRenderUpdate(message.blockPos, message.blockPos);
                }
            }
        });
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
