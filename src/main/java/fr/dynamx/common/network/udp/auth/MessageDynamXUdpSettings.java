package fr.dynamx.common.network.udp.auth;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.DynamXClientNetworkSystem;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageDynamXUdpSettings implements IDnxPacket, IMessageHandler<MessageDynamXUdpSettings, IMessage>
{
    private int voiceServerType;
    private int udpPort;
    private String hash;
    private String ip;
    private boolean syncDynamXPacks;

    public MessageDynamXUdpSettings() {}
    public MessageDynamXUdpSettings(int voiceServerType, int udpPort, String hash, String ip, boolean syncDynamXPacks)
    {
        this.voiceServerType = voiceServerType;
        this.udpPort = udpPort;
        this.hash = hash;
        this.ip = ip;
        this.syncDynamXPacks = syncDynamXPacks;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Read auth proposal");
        this.voiceServerType = buf.readInt();
        this.udpPort = buf.readInt();
        this.hash = ByteBufUtils.readUTF8String(buf);
        this.ip = ByteBufUtils.readUTF8String(buf);
        this.syncDynamXPacks = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Write auth proposal");
        buf.writeInt(this.voiceServerType);
        buf.writeInt(this.udpPort);
        ByteBufUtils.writeUTF8String(buf, this.hash);
        ByteBufUtils.writeUTF8String(buf, this.ip);
        buf.writeBoolean(syncDynamXPacks);
    }

    @Override
    public IMessage onMessage(final MessageDynamXUdpSettings packet, MessageContext ctx)
    {
        if(DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Received auth proposal");
        DynamXConfig.syncPacks = packet.syncDynamXPacks;
        Minecraft.getMinecraft().addScheduledTask(() -> ((DynamXClientNetworkSystem) DynamXContext.getNetwork()).startNetwork(EnumNetworkType.values()[packet.voiceServerType], packet.hash, packet.ip, packet.udpPort));
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}