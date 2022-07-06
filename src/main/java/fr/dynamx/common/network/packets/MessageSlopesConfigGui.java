package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.items.tools.ItemSlopes;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSlopesConfigGui implements IDnxPacket, IMessageHandler<MessageSlopesConfigGui, IMessage>
{
    private NBTTagCompound serializedConfig;

    public MessageSlopesConfigGui() {}

    public MessageSlopesConfigGui(NBTTagCompound serializedConfig) {
        this.serializedConfig = serializedConfig;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        serializedConfig = ByteBufUtils.readTag(byteBuf);
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        ByteBufUtils.writeTag(byteBuf, serializedConfig);
    }

    @Override
    public IMessage onMessage(MessageSlopesConfigGui messageSlopesConfigGui, MessageContext messageContext) {
        ItemStack stack = messageContext.getServerHandler().player.getHeldItemMainhand();
        if(stack.getItem() instanceof ItemSlopes)
        {
            if(!stack.hasTagCompound())
                stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setTag("ptconfig", messageSlopesConfigGui.serializedConfig);
        }
        return null;
    }
}
