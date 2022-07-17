package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.items.tools.WrenchMode;
import fr.dynamx.common.physics.PhysicsTickHandler;
import fr.dynamx.utils.DynamXConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageDebugRequest implements IDnxPacket, IMessageHandler<MessageDebugRequest, IMessage> {
    public int debugMode;

    public MessageDebugRequest() {
    }

    /**
     * @param debugMode A terrain debug mode id, OR -15815 to switch slopes item mode, OR -15816 to switch wrench item mode
     */
    public MessageDebugRequest(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(debugMode);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        debugMode = buf.readInt();
    }

    @Override
    public IMessage onMessage(MessageDebugRequest message, MessageContext ctx) {
        if (message.debugMode == -15815) {
            ItemStack s = ctx.getServerHandler().player.getHeldItemMainhand();
            if (s.getItem() instanceof ItemSlopes) {
                if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
                if (s.getTagCompound().getInteger("mode") == 0) {
                    s.getTagCompound().setInteger("mode", 1);
                    ctx.getServerHandler().player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Mode mis à [CREATE]"));
                } else if (s.getTagCompound().getInteger("mode") == 1) {
                    s.getTagCompound().setInteger("mode", 2);
                    ctx.getServerHandler().player.sendMessage(new TextComponentString(TextFormatting.GOLD + "Mode mis à [AUTO]"));
                } else {
                    s.getTagCompound().setInteger("mode", 0);
                    ctx.getServerHandler().player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "Mode mis à [DELETE]"));
                }
            }
        }
        if (message.debugMode == -15816) {
            ItemStack s = ctx.getServerHandler().player.getHeldItemMainhand();
            if (s.getItem() instanceof ItemWrench) {
                WrenchMode.switchMode(ctx.getServerHandler().player, s);
            }
        } else if (message.debugMode <= -15817) {
            int mode = (message.debugMode + 15817) * (-1);
            ItemStack s = ctx.getServerHandler().player.getHeldItemMainhand();
            if (s.getItem() instanceof ItemWrench) {
                WrenchMode.setMode(ctx.getServerHandler().player, s, mode);
            }
        } else {
            if (ctx.getServerHandler().player.canUseCommand(2, DynamXConstants.ID + ".command.debug_gui")) {
                ctx.getServerHandler().player.getServer().addScheduledTask(() -> PhysicsTickHandler.requestedDebugInfo.put(ctx.getServerHandler().player, message.debugMode));
            } else {
                DynamXMain.log.warn(ctx.getServerHandler().player + " tried to enable debug mode " + message.debugMode + " while not in the debug gui");
                ctx.getServerHandler().player.sendMessage(new TextComponentString("[DynamX] You are not allowed to use server debug !"));
            }
        }
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}