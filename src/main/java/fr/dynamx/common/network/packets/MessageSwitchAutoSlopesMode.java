package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.ContentPackLoader;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageSwitchAutoSlopesMode implements IDnxPacket
{
    public int mode;

    public MessageSwitchAutoSlopesMode(int mode) {
        this.mode = mode;
    }

    public MessageSwitchAutoSlopesMode() {}

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode= buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode);
    }

    public static class Handler implements IMessageHandler<MessageSwitchAutoSlopesMode, IDnxPacket>
    {
        @Override
        public IDnxPacket onMessage(MessageSwitchAutoSlopesMode message, MessageContext ctx) {
            handleClient(message,ctx);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(MessageSwitchAutoSlopesMode message, MessageContext ctx) {
            ContentPackLoader.PLACE_SLOPES = message.mode == 1;
        }
    }
}
