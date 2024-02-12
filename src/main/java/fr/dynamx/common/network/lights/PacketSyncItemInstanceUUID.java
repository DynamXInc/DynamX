package fr.dynamx.common.network.lights;

import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.lights.ItemLightContainer;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class PacketSyncItemInstanceUUID implements IMessage {

    private UUID id;

    public PacketSyncItemInstanceUUID() {
    }

    public PacketSyncItemInstanceUUID(UUID id) {
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {

        id = UUID.fromString(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {

        ByteBufUtils.writeUTF8String(buf, id.toString());
    }

    public static class ClientHandler implements IMessageHandler<PacketSyncItemInstanceUUID, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncItemInstanceUUID message, MessageContext ctx) {
            DynamXItem.itemInstanceLights.put(message.id, new AbstractLightsModule.ItemLightsModule(null, null, message.id));
            return null;
        }
    }
}
