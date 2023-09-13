package fr.dynamx.common.network.packets;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.client.gui.GuiDnxDebug;
import fr.dynamx.client.gui.NewGuiDnxDebug;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageOpenDebugGui implements IDnxPacket {
    private byte action;

    public MessageOpenDebugGui() {
    }

    public MessageOpenDebugGui(byte action) {
        this.action = action;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    public static class Handler implements IMessageHandler<MessageOpenDebugGui, IMessage> {
        @Override
        public IMessage onMessage(MessageOpenDebugGui message, MessageContext ctx) {
            /*if (message.action == -20) {
                //CmdOpenDebugGui.openedGuis.remove(ctx.getServerHandler().player);
            }
            else*/
            if (message.action == 125) {
                ACsGuiApi.asyncLoadThenShowGui("Dnx Debug", NewGuiDnxDebug::new);
            }
            return null;
        }
    }
}
