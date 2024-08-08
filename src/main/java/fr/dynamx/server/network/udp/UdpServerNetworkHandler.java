package fr.dynamx.server.network.udp;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.common.network.udp.UDPPacket;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtils;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

/**
 * Udp server manager, on server side
 */
public class UdpServerNetworkHandler implements IDnxNetworkHandler {
    public final Map<String, EntityPlayerMP> waitingAuth = new HashMap<>();

    public static volatile boolean running;
    private UDPServerPacketHandler handler;
    public Map<Integer, UDPClient> clientMap;
    private UdpServer server;

    public UdpServerNetworkHandler() {
    }

    public void closeConnection(int id) {
        UDPClient client = this.clientMap.get(id);
        if (client != null) {
            this.handler.closeConnection(client.socketAddress);
        }
        this.clientMap.remove(id);
    }

    public void sendPacket(UDPPacket packet, UDPClient client) {
        ByteBuf packetBuffer = Unpooled.buffer();
        packetBuffer.writeByte(packet.id());
        packet.write(packetBuffer);
        byte[] data = packetBuffer.array();

        try {
            this.server.send(new DatagramPacket(data, data.length, client.socketAddress));
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-DEBUG] Sent the packet " + packet.id());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean start() {
        this.clientMap = new HashMap<>();
        this.handler = new UDPServerPacketHandler(this);
        MinecraftServer mc = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (mc.isDedicatedServer()) {
            if (StringUtils.isNullOrEmpty(mc.getServerHostname())) {
                this.server = new UdpServer(DynamXMain.log, DynamXConfig.udpPort);
            } else {
                this.server = new UdpServer(DynamXMain.log, mc.getServerHostname(), DynamXConfig.udpPort);
                //if(DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-Server] Applied custom IP " + mc.getServerHostname());
                //this.server = new UdpServer(DynamXMain.log, DynamXConfig.udpPort);
            }
        } else {
            this.server = new UdpServer(DynamXMain.log, "localhost", DynamXConfig.udpPort);
        }

        this.server.addUdpServerListener(evt -> {
            //if(DynamXConfig.udpDebug)
            //  DynamXMain.log.info("[UDP-DEBUG] RCV packet event !");
            try {
                UdpServerNetworkHandler.this.handler.read(evt.getPacketAsBytes(), evt.getPacket());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        this.server.start();
        return true;
    }

    @Override
    public void stop() {
        if (running) {
            running = false;
            if (handler != null)
                this.handler.close();
            this.server.clearUdpListeners();
            this.server.stop();
            this.clientMap.clear();
            this.handler = null;
            this.server = null;
        }
    }

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        UDPPacket pck = new EncapsulatedUDPPacket(packet);
        if (EnumPacketTarget.SERVER == targetType) {
            throw new IllegalArgumentException("Cannot send a packet to the server, from the server !");
        } else if (EnumPacketTarget.PLAYER == targetType) {
            UDPClient client = clientMap.get(((Entity) target).getEntityId());
            if (client == null)
                vanillaFallback(packet, (EntityPlayerMP) target);
            else
                sendPacket(pck, client);
        } else if (EnumPacketTarget.ALL_AROUND == targetType) {
            throw new UnsupportedOperationException("Not implemented yet in UDP, please contact DynamX devs");
        } else if (EnumPacketTarget.ALL_TRACKING_ENTITY == targetType) {
            WorldServer world = (WorldServer) ((Entity) target).world;
            world.getEntityTracker().getTrackingPlayers((Entity) target).forEach(player -> {
                UDPClient client = clientMap.get(player.getEntityId());
                if (client == null)
                    vanillaFallback(packet, (EntityPlayerMP) player);
                else
                    sendPacket(pck, client);
            });
        } else if (EnumPacketTarget.ALL == targetType) {
            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers().forEach(player -> {
                UDPClient client = clientMap.get(player.getEntityId());
                if (client == null)
                    vanillaFallback(packet, player);
                else
                    sendPacket(pck, client);
            });
        }
    }

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    private void vanillaFallback(IDnxPacket packet, EntityPlayerMP target) {
        if (target.connection != null && target.connection.getNetworkManager().isChannelOpen()) {
            DynamXContext.getNetwork().getVanillaNetwork().sendPacket(packet, EnumPacketTarget.PLAYER, target);
        }
    }
}
