package fr.dynamx.common.network;

import fr.dynamx.api.network.*;
import fr.dynamx.client.network.udp.UdpClientNetworkHandler;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Client (and single player) network system
 */
public class DynamXClientNetworkSystem implements IDnxNetworkSystem {
    private final VanillaNetworkHandler VANILLA_NETWORK;
    private IDnxNetworkHandler QUICK_NETWORK;
    private Thread clientHandlerThread;
    public boolean connected;

    /**
     * @param networkType Preferred network type
     */
    public DynamXClientNetworkSystem(EnumNetworkType networkType) {
        Runtime.getRuntime().addShutdownHook(new Thread(DynamXClientNetworkSystem.this::stopNetwork));
        VANILLA_NETWORK = new VanillaNetworkHandler();
        switch (networkType) {
            case VANILLA_TCP:
            case DYNAMX_UDP:
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            default:
                throw new UnsupportedOperationException("Network type " + networkType + " isn't supported for the moment !");
        }
    }

    @Override
    public void sendToServer(IDnxPacket packet) {
        if (packet.getPreferredNetwork() == EnumNetworkType.VANILLA_TCP) {
            VANILLA_NETWORK.sendPacket(packet, EnumPacketTarget.SERVER, null);
        } else {
            QUICK_NETWORK.sendPacket(packet, EnumPacketTarget.SERVER, null);
        }
    }

    @Override
    public <T> void sendToClient(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        VANILLA_NETWORK.sendPacket(packet, targetType, target);
    }

    @Override
    public VanillaNetworkHandler getVanillaNetwork() {
        return VANILLA_NETWORK;
    }

    /**
     * @return True if connected to a remote server
     */
    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void startNetwork() {
    }

    /**
     * Called to open connection with the remote server
     *
     * @param type    The network type to create
     * @param hash    The connection password
     * @param ip      The server ip
     * @param udpPort The server port
     */
    public void startNetwork(EnumNetworkType type, String hash, String ip, int udpPort) {
        if (this.isConnected()) {
            this.stopNetwork();
        }

        switch (type) {
            case VANILLA_TCP:
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            case DYNAMX_UDP:
                String serverAddress = ip;
                if (ip.isEmpty()) {
                    ServerData serverData;
                    if ((serverData = Minecraft.getMinecraft().getCurrentServerData()) != null) {
                        ServerAddress server = ServerAddress.fromString(serverData.serverIP);
                        serverAddress = server.getIP();
                        //DynamXMain.log.info("Server IP is "+serverAddress);
                    } else {
                        SocketAddress address = Minecraft.getMinecraft().getConnection().getNetworkManager().getRemoteAddress();
                        if (address instanceof InetSocketAddress) {
                            serverAddress = ((InetSocketAddress) address).getAddress().getHostAddress();
                            //DynamXMain.log.info("Found server IP "+serverAddress);
                        } else {
                            DynamXMain.log.fatal("Cannot find current server IP address, using default IP, localhost !");
                            serverAddress = "localhost";
                        }
                    }
                }
                //else
                // System.out.println("Server sent ip "+serverAddress);
                if (DynamXConfig.udpDebug)
                    DynamXMain.log.info("[UDP-DEBUG] Authing with " + serverAddress);
                QUICK_NETWORK = new UdpClientNetworkHandler(hash, serverAddress, udpPort);
                break;
            default:
                throw new UnsupportedOperationException("UDP client type " + type + " not supported yet");
        }

        if (QUICK_NETWORK != VANILLA_NETWORK) {
            //This thread will handle packets reception
            this.clientHandlerThread = new Thread(() -> QUICK_NETWORK.start(), "DynamX UDP Client Receiver");
            this.clientHandlerThread.setDaemon(QUICK_NETWORK instanceof UdpClientNetworkHandler);
            this.clientHandlerThread.start();
        }
        this.connected = true;
        DynamXMain.log.info("Connected to [" + type + "] Server.");
    }

    @Override
    public void stopNetwork() {
        this.connected = false;

        if (QUICK_NETWORK != VANILLA_NETWORK) {
            QUICK_NETWORK.stop();
            DynamXMain.log.info("Stopped " + QUICK_NETWORK.getType() + " Client.");
        }

        if (this.clientHandlerThread != null) {
            this.clientHandlerThread.interrupt();
        }
        this.clientHandlerThread = null;
        this.QUICK_NETWORK = VANILLA_NETWORK;

        VANILLA_NETWORK.stop();
    }

    @Override
    public IDnxNetworkHandler getQuickNetwork() {
        return QUICK_NETWORK;
    }
}