package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

public class MessageSyncPlayerPicking implements IDnxPacket, IMessageHandler<MessageSyncPlayerPicking, IDnxPacket> {

    private Map<Integer, Integer> map = new HashMap<>();

    public MessageSyncPlayerPicking() {
    }

    public MessageSyncPlayerPicking(Map<Integer, Integer> map) {
        this.map = map;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            map.put(buf.readInt(), buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(map.size());
        map.forEach((integer, integer2) -> {
            buf.writeInt(integer);
            buf.writeInt(integer2);
        });
    }


    @Override
    public IDnxPacket onMessage(MessageSyncPlayerPicking message, MessageContext ctx) {
        DynamXContext.setPlayerPickingObjects(message.map);
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
