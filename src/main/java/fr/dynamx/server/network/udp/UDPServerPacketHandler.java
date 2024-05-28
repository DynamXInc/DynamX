package fr.dynamx.server.network.udp;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.common.network.udp.UdpTestPacket;
import fr.dynamx.common.network.udp.auth.UDPServerAuthenticationCompletePacket;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Low-level packet handler
 */
public class UDPServerPacketHandler {
    private final ExecutorService threadService;
    private final Map<InetSocketAddress, UDPClient> clientNetworkMap = new HashMap<>();
    private final UdpServerNetworkHandler server;

    public UDPServerPacketHandler(UdpServerNetworkHandler server) {
        this.server = server;
        this.threadService = Executors.newFixedThreadPool(1);//Executors.newFixedThreadPool((int) MathHelper.clamp((float)FMLCommonHandler.instance().getMinecraftServerInstance().getMaxPlayers(), 1.0F, 1.0F));
        //TODOOLD low aym bigger pool, a day => problems
        //TODO TEST MASSIVE SEND OF PACKET TO SEE SPEED OF HANDLING
    }

    public void close() {
        this.clientNetworkMap.clear();
        this.threadService.shutdown();
    }

    public void closeConnection(InetSocketAddress address) {
        this.clientNetworkMap.remove(address);
    }

    private void handleAuthentication(InetSocketAddress address, DatagramPacket packet, ByteBuf in) {
        final String hash = ByteBufUtils.readUTF8String(in);
        final EntityPlayerMP player = this.server.waitingAuth.remove(hash);

        if (player != null) {
            UDPClient client = new UDPClient(player, address, hash);
            this.clientNetworkMap.put(client.socketAddress, client);
            this.server.clientMap.put(player.getEntityId(), client);
            DynamXMain.log.info(client + " has been authenticated by server.");
            this.server.sendPacket(new UDPServerAuthenticationCompletePacket(), client);
        } else
            DynamXMain.log.warn("Cannot authenticate a client : not waiting for auth");
    }

    public void read(byte[] data, final DatagramPacket packet) {
        final InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();
        final UDPClient client = this.clientNetworkMap.get(address);
        final ByteBuf in = Unpooled.wrappedBuffer(data);
        final byte id = in.readByte();

        if (DynamXConfig.udpDebug) {
            if (client != null)
                DynamXMain.log.info("[UDP-DEBUG] Read packet with id " + id + " from " + client.player);
            else
                DynamXMain.log.error("[UDP-DEBUG] Read packet with id " + id + " but client is null..." + packet.getAddress());
        }
        this.threadService.execute(() -> {
            if (id == 0) {
                UDPServerPacketHandler.this.handleAuthentication(address, packet, in);
            } else if (id == 9) {
                UdpTestPacket packet2 = new UdpTestPacket(in.readInt(), ByteBufUtils.readUTF8String(in), in.readLong(), in.readLong() == -1 ? System.currentTimeMillis() : -2);
                server.sendPacket(packet2, client);
            } else if (client != null) {
                if (id >= 10) {
                    EncapsulatedUDPPacket.readAndHandle(id, in, client.player);
                } else {
                    throw new IllegalArgumentException("Illegal dynamx packet id " + id);
                }
            }
        });
    }
}